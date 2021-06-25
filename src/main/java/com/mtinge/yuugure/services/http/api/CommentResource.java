package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.core.comments.Renderer;
import com.mtinge.yuugure.data.http.*;
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
                var report = App.database().createReport(resource.resource, authed, frmReason.getValue());
                if (report != null) {
                  res.json(Response.good().addData(ReportResponse.class, ReportResponse.fromDb(report)));
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
          res.json(Response.good().addData(SafeComment.class, SafeComment.fromDb(comment.resource)));
        } else if (method.equals(Methods.DELETE)) {
          // TODO people with elevated roles/permissions (moderators/etc) should be able to delete.
          if (authed == null || authed.id != comment.resource.account) {
            res.status(StatusCodes.UNAUTHORIZED).json(Response.bad(StatusCodes.UNAUTHORIZED, StatusCodes.UNAUTHORIZED_STRING));
          } else {
            var updated = App.database().jdbi().withHandle(handle ->
              handle.createUpdate("UPDATE comment SET active = false WHERE id = :id")
                .bind("id", comment.resource.id)
                .execute()
            );

            var code = updated > 0 ? StatusCodes.OK : StatusCodes.INTERNAL_SERVER_ERROR;
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
          res.json(Response.good().addAll(RenderableComment.class, App.database().getRenderableCommentsForUpload(upload.resource.id, false)));
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
                    var comment = App.database().createComment(upload.resource, authed, body, rendered);
                    if (comment != null) {
                      var renderable = App.database().makeCommentRenderable(comment);
                      response.setComment(renderable);
                      App.webServer().wsListener().getLobby().in("upload:" + upload.resource.id).broadcast(OutgoingPacket.prepare("comment").addData("comment", renderable));
                    } else {
                      response.addError("An internal server error occurred. Please try again later.");
                      code = StatusCodes.INTERNAL_SERVER_ERROR;
                    }
                  }
                }

                if (!exchange.isResponseStarted()) {
                  res.status(code).json(Response.fromCode(code).addData(CommentResponse.class, response));
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
    var authed = getAuthed(exchange);
    var uid = extractInt(exchange, "upload");

    if (uid != null) {
      var upload = App.database().getUploadById(uid, false);
      if (upload != null) {
        if (States.flagged(upload.state, States.Upload.PRIVATE) && (authed == null || authed.id != upload.owner)) {
          return ResourceResult.unauthorized();
        }
        return ResourceResult.OK(upload);
      }
    }

    return ResourceResult.notFound();
  }

  @Override
  protected ResourceResult<DBComment> fetchResource(HttpServerExchange exchange) {
    var id = extractInt(exchange, "comment");
    if (id != null) {
      var comment = App.database().getCommentById(id, false);
      if (comment != null && comment.active) {
        return ResourceResult.OK(comment);
      }
    }

    return ResourceResult.notFound();
  }
}
