package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.RenderableUpload;
import com.mtinge.yuugure.data.http.ReportResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.SafeAccount;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBMediaMeta;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;
import io.undertow.util.PathTemplateMatch;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteAPI extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteAPI.class);

  private final PathHandler pathHandler;

  public RouteAPI() {
    super();
    this.pathHandler = Handlers.path()
      .addPrefixPath("/upload", Handlers.pathTemplate()
        .add("/{upload}", this::getUpload)
        .add("/{upload}/{action}", this::handleUploadAction)
      )
      .addPrefixPath("/account", Handlers.pathTemplate()
        .add("/{account}", this::getAccount)
        .add("/{account}/{action}", this::handleAccountAction)
      );
  }

  private void getUpload(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var authedAccount = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    var params = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    var uploadId = params.getParameters().get("upload");
    var notFound = Response.bad(StatusCodes.NOT_FOUND, StatusCodes.NOT_FOUND_STRING);

    if (uploadId != null) {
      if (!uploadId.matches("^[0-9]+$")) {
        res.status(StatusCodes.BAD_REQUEST).json(Response.bad(StatusCodes.BAD_REQUEST, StatusCodes.BAD_REQUEST_STRING));
      } else {
        // Fetch the requested upload, filtering it out if it has any non-fetchable state.
        var upload = App.database().jdbi().withHandle(handle ->
          handle.createQuery("SELECT * FROM upload WHERE id = :id AND (state & :badFlags) = 0")
            .bind("id", Integer.parseInt(uploadId))
            .bind("badFlags", States.addFlag(0L, States.Upload.DELETED, States.Upload.DMCA))
            .map(DBUpload.Mapper)
            .findFirst().orElse(null)
        );
        if (upload != null) {
          boolean canRender = true;
          if (States.flagged(upload.state, States.Upload.PRIVATE)) {
            if (authedAccount == null) {
              res.status(StatusCodes.UNAUTHORIZED).json(Response.bad(StatusCodes.UNAUTHORIZED, StatusCodes.UNAUTHORIZED_STRING));
              canRender = false;
            } else if (authedAccount.id != upload.owner) {
              res.status(StatusCodes.FORBIDDEN).json(Response.bad(StatusCodes.FORBIDDEN, StatusCodes.FORBIDDEN_STRING));
              canRender = false;
            }
          }

          if (canRender) {
            var toSend = App.database().jdbi().withHandle(handle -> {
              var media = handle.createQuery("SELECT * FROM media WHERE id = :id")
                .bind("id", upload.media)
                .map(DBMedia.Mapper)
                .first();
              var meta = handle.createQuery("SELECT * FROM media_meta WHERE media = :id")
                .bind("id", media.id)
                .map(DBMediaMeta.Mapper)
                .findFirst().orElse(null);
              var owner = handle.createQuery("SELECT * FROM account WHERE id = :id")
                .bind("id", upload.owner)
                .map(DBAccount.Mapper)
                .map(SafeAccount::fromDb)
                .first();
              return new RenderableUpload(upload, media, meta, owner);
            });
            res.json(Response.good().addData(RenderableUpload.class, toSend));
          }
        } else {
          res.status(StatusCodes.NOT_FOUND).json(notFound);
        }
      }
    } else {
      res.status(StatusCodes.NOT_FOUND).json(notFound);
    }
    exchange.endExchange();
  }

  private void handleUploadAction(HttpServerExchange exchange) {
    var res = Responder.with(exchange);

    var account = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    if (account == null) {
      res.notAuthorized();
      return;
    }

    var matches = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    if (matches != null) {
      var action = matches.getParameters().get("action");
      if (action == null || action.isBlank()) {
        // handle firefox adding a trailing slash
        getUpload(exchange);
      } else {
        var uploadId = matches.getParameters().get("upload");
        if (uploadId != null && !uploadId.isBlank() && uploadId.matches("^[0-9]+$")) {
          var upload = App.database().getUploadById(Integer.parseInt(uploadId));
          if (upload != null) {
            switch (action.trim().toLowerCase()) {
              case "report" -> {
                if (validateMethods(exchange, Methods.POST)) {
                  var body = exchange.getAttachment(FormDataParser.FORM_DATA);
                  if (body != null) {
                    var frmReason = body.getFirst("reason");
                    if (frmReason != null && !frmReason.isFileItem()) {
                      var report = App.database().createReport(upload, account, frmReason.getValue());
                      if (report != null) {
                        res.json(Response.good().addData(ReportResponse.class, ReportResponse.fromDb(report)));
                      } else {
                        res.internalServerError();
                        logger.error("Report returned from database on upload {} from user {} was null.", upload.id, account.id);
                      }
                    } else {
                      res.badRequest();
                    }
                  } else {
                    res.badRequest();
                  }
                }
              }
            }
          } else {
            res.notFound();
          }
        } else {
          res.badRequest();
        }
      }
    } else {
      res.badRequest();
    }
  }

  private void getAccount(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var authed = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    var match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    if (match != null) {
      var aId = match.getParameters().get("account");
      if (aId != null && aId.matches("^[0-9]+$")) {
        var account = App.database().getAccountById(Integer.parseInt(aId));
        if (account != null) {
          // if the account is private, ensure the calling user has permission to view the resource
          if (States.flagged(account.state, States.User.PRIVATE)) {
            if (authed != null && authed.id == account.id) {
              res.json(SafeAccount.fromDb(account));
            } else {
              if (authed == null) {
                res.notAuthorized();
              } else {
                res.forbidden();
              }
            }
          } else {
            res.json(SafeAccount.fromDb(account));
          }
        } else {
          res.notFound();
        }
      } else {
        res.notFound();
      }
    } else {
      res.badRequest();
    }
  }

  private void handleAccountAction(HttpServerExchange exchange) {
    var res = Responder.with(exchange);

    var authed = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    if (authed == null) {
      res.notAuthorized();
      return;
    }

    var match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    if (match != null && match.getParameters().get("account") != null && match.getParameters().get("account").matches("^[0-9]+$")) {
      var action = match.getParameters().get("action");
      if (action != null && !action.isBlank()) {
        var account = App.database().getAccountById(Integer.parseInt(match.getParameters().get("account")));
        if (account != null) {
          switch (action.toLowerCase().trim()) {
            case "report" -> {
              if (validateMethods(exchange, Methods.POST)) {
                var body = exchange.getAttachment(FormDataParser.FORM_DATA);
                if (body != null) {
                  var frmReason = body.getFirst("reason");
                  if (frmReason != null && !frmReason.isFileItem()) {
                    var report = App.database().createReport(account, authed, frmReason.getValue());
                    if (report != null) {
                      res.json(Response.good().addData(ReportResponse.class, ReportResponse.fromDb(report)));
                    } else {
                      res.internalServerError();
                      logger.error("Report returned from database on account {} from user {} was null.", account.id, account.id);
                    }
                  } else {
                    res.badRequest();
                  }
                } else {
                  res.badRequest();
                }
              }
            }
          }
        } else {
          res.notFound();
        }
      } else {
        // handle firefox adding trailing slash
        getAccount(exchange);
      }
    } else {
      res.badRequest();
    }
    res.end();
  }

  @Override
  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/api", pathHandler);
  }
}
