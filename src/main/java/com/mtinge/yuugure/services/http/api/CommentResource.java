package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.core.Strings;
import com.mtinge.yuugure.core.comments.Renderer;
import com.mtinge.yuugure.data.http.CommentResponse;
import com.mtinge.yuugure.data.http.ReportResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.SafeComment;
import com.mtinge.yuugure.data.postgres.DBComment;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.util.MethodValidator;
import com.mtinge.yuugure.services.http.ws.packets.OutgoingPacket;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommentResource extends APIResource<DBComment> {
  private static final Logger logger = LoggerFactory.getLogger(CommentResource.class);

  @Override
  public PathTemplateHandler getRoutes() {
    return Handlers.pathTemplate()
      .add("/upload/{upload}", this::handleUploadComment)
      .add("/{comment}", this::handleDirectComment)
      .add("/{comment}/{action}", this::handleCommentAction);
  }

  private void handleCommentAction(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var authed = getAuthed(exchange);
    var action = extract(exchange, "action");
    if (action.isBlank()) {
      // catch firefox adding a trailing slash to requests
      handleDirectComment(exchange);
      return;
    }
    if (authed == null) {
      res.status(StatusCodes.UNAUTHORIZED).json(Response.fromCode(StatusCodes.UNAUTHORIZED));
      return;
    }

    var resource = fetchResource(exchange);
    if (resource.state == FetchState.OK) {
      if (action.equalsIgnoreCase("report")) {
        if (MethodValidator.handleMethodValidation(exchange, Methods.POST)) {
          var body = exchange.getAttachment(FormDataParser.FORM_DATA);
          if (body != null) {
            var frmReason = body.getFirst("reason");
            if (frmReason != null && !frmReason.isFileItem()) {
              if (checkRatelimit(exchange, App.webServer().limiters().reportLimiter())) {
                var report = App.database().jdbi().inTransaction(handle -> App.database().reports.create(resource.resource, authed, frmReason.getValue(), handle));
                if (report.isSuccess()) {
                  res.json(Response.good(ReportResponse.fromDb(report.getResource())));
                } else {
                  res.internalServerError();
                  logger.error("Report returned from database on comment {} from user {} was null.", resource.resource.id, resource.resource.id);
                }
              }
            } else {
              res.badRequest();
            }
          } else {
            res.badRequest();
          }
        }
      } else {
        res.status(StatusCodes.NOT_FOUND).json(Response.fromCode(StatusCodes.NOT_FOUND));
      }
    } else {
      sendTerminalForState(exchange, resource.state);
    }
  }

  private void handleDirectComment(HttpServerExchange exchange) {
    var authed = getAuthed(exchange);
    var res = Responder.with(exchange);
    var comment = fetchResource(exchange);
    if (comment.state == FetchState.OK) {
      if (MethodValidator.handleMethodValidation(exchange, Methods.GET, Methods.DELETE)) {
        var method = exchange.getRequestMethod();
        if (method.equals(Methods.GET)) {
          res.json(Response.good(SafeComment.fromDb(comment.resource)));
        } else if (method.equals(Methods.DELETE)) {
          if (authed == null || (!authed.hasModPerms() && authed.id != comment.resource.account)) {
            res.status(StatusCodes.UNAUTHORIZED).json(Response.fromCode(StatusCodes.UNAUTHORIZED));
          } else {
            boolean isAdminAction = authed.id != comment.resource.account;
            String reason = null;
            if (isAdminAction) {
              reason = extractForm(exchange, "reason");
              if (reason.isBlank()) {
                res.status(StatusCodes.BAD_REQUEST).json(Response.fromCode(StatusCodes.BAD_REQUEST).addMessage("Missing reason."));
                return;
              }
            }

            // finalized for lambda
            final String _reason = reason;
            var deleted = App.database().jdbi().withHandle(handle -> {
              var _ret = App.database().comments.delete(comment.resource.id, handle);

              if (isAdminAction && _ret.isSuccess()) {
                App.database().audits.trackAction(authed, comment.resource, "delete", "Admin supplied reason: " + _reason, handle);
              }

              return _ret;
            });

            var code = deleted.isSuccess() ? StatusCodes.OK : StatusCodes.INTERNAL_SERVER_ERROR;
            res.status(code).json(Response.fromCode(code));
          }
        }
      }
    } else {
      sendTerminalForState(exchange, comment.state);
    }
  }

  private void handleUploadComment(HttpServerExchange exchange) {
    var res = Responder.with(exchange);

    var upload = fetchUpload(exchange);
    if (upload.state == FetchState.OK) {
      if (MethodValidator.handleMethodValidation(exchange, Methods.GET, Methods.POST, Methods.DELETE, Methods.PATCH)) {
        var method = exchange.getRequestMethod();
        if (method.equals(Methods.GET)) {
          // Fetch comments
          var comments = App.database().jdbi().inTransaction(handle -> App.database().comments.makeRenderable(App.database().comments.readForUpload(upload.resource, false, handle), handle));
          res.json(Response.good(comments));
        } else {
          // Comment actions - delete, report, edit, etc.
          var authed = getAuthed(exchange);
          if (authed != null) {
            if (method.equals(Methods.POST)) {
              // Create new comment
              if (!States.flagged(authed.state, States.Account.COMMENTS_RESTRICTED)) {
                var response = new CommentResponse();
                int code = StatusCodes.OK;

                var body = extractForm(exchange, "body");
                if (body.isBlank()) {
                  response.addInputError("body", "This field is required.");
                  code = StatusCodes.BAD_REQUEST;
                } else {
                  if (checkRatelimit(exchange, App.webServer().limiters().commentLimiter())) {
                    var rendered = Renderer.render(body);
                    var comment = App.database().jdbi().withHandle(handle -> {
                      var _new = App.database().comments.create(upload.resource, authed, body, rendered, handle);
                      if (_new.isSuccess()) {
                        return App.database().comments.makeRenderable(_new.getResource(), handle);
                      }
                      return null;
                    });
                    if (comment != null) {
                      response.setComment(comment);
                      App.webServer().wsListener().getLobby().in("upload:" + upload.resource.id).broadcast(OutgoingPacket.prepare("comment").addData("comment", comment));
                    } else {
                      response.addError(Strings.Generic.INTERNAL_SERVER_ERROR);
                      code = StatusCodes.INTERNAL_SERVER_ERROR;
                    }
                  }
                }

                if (!exchange.isResponseStarted()) {
                  res.status(code).json(Response.fromCode(code, response));
                }
              } else {
                res.status(StatusCodes.FORBIDDEN).json(Response.fromCode(StatusCodes.FORBIDDEN).addMessage("You have been restricted from creating new comments."));
              }
            } else if (method.equals(Methods.DELETE)) {
              // Delete comment
              res.status(StatusCodes.NOT_IMPLEMENTED).json(Response.fromCode(StatusCodes.NOT_IMPLEMENTED));
            } else if (method.equals(Methods.PATCH)) {
              // Update comment
              res.status(StatusCodes.NOT_IMPLEMENTED).json(Response.fromCode(StatusCodes.NOT_IMPLEMENTED));
            }
          } else {
            res.status(StatusCodes.UNAUTHORIZED).json(Response.fromCode(StatusCodes.UNAUTHORIZED));
          }
        }
      }
    }
  }

  protected ResourceResult<DBUpload> fetchUpload(HttpServerExchange exchange) {
    return UploadResource.getUpload(exchange, "upload");
  }

  @Override
  protected ResourceResult<DBComment> fetchResource(HttpServerExchange exchange) {
    var id = extractInt(exchange, "comment");
    if (id != null) {
      var comment = App.database().jdbi().withHandle(handle -> App.database().comments.read(id, handle));
      if (comment != null && comment.active) {
        return ResourceResult.OK(comment);
      }
    }

    return ResourceResult.notFound();
  }
}
