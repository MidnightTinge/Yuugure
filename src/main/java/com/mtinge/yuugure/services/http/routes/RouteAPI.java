package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.RenderableUpload;
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
import io.undertow.util.PathTemplateMatch;
import io.undertow.util.StatusCodes;

public class RouteAPI extends Route {
  private final PathHandler pathHandler;

  public RouteAPI() {
    super();
    this.pathHandler = Handlers.path()
      .addPrefixPath("/upload", Handlers.pathTemplate().add("/{upload}", this::getUpload));
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

  @Override
  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/api", pathHandler);
  }
}
