package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.ReportResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.http.Responder;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadResource extends APIResource<DBUpload> {
  private static final Logger logger = LoggerFactory.getLogger(UploadResource.class);

  @Override
  public PathTemplateHandler getRoutes() {
    return Handlers.pathTemplate()
      .add("/index", this::fetchForIndex)
      .add("/{upload}", this::handleFetch)
      .add("/{upload}/{action}", this::handleAction);
  }

  private void fetchForIndex(HttpServerExchange exchange) {
    Responder.with(exchange).json(Response.good(App.database().getIndexUploads(getAuthed(exchange))));
  }

  private void handleFetch(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var resource = fetchResource(exchange);
    if (resource.state == FetchState.OK) {
      var renderable = App.database().makeUploadRenderable(resource.resource);
      res.json(Response.good(renderable));
    } else {
      sendTerminalForState(exchange, resource.state);
    }
  }

  private void handleAction(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var action = extract(exchange, "action");
    if (action.isBlank()) {
      this.handleFetch(exchange);
      return;
    }
    var authed = getAuthed(exchange);
    if (authed == null) {
      res.notAuthorized();
      return;
    }

    var resource = fetchResource(exchange);
    if (resource.state == FetchState.OK) {
      if (action.trim().equalsIgnoreCase("report")) {
        if (exchange.getRequestMethod().equals(Methods.POST)) {
          if (checkRatelimit(exchange, App.webServer().limiters().reportLimiter())) {
            var body = exchange.getAttachment(FormDataParser.FORM_DATA);
            if (body != null) {
              var frmReason = body.getFirst("reason");
              if (frmReason != null && !frmReason.isFileItem()) {
                var report = App.database().createReport(resource.resource, authed, frmReason.getValue());
                if (report != null) {
                  res.json(Response.good(ReportResponse.fromDb(report)));
                } else {
                  res.internalServerError();
                  logger.error("Report returned from database on upload {} from user {} was null.", resource.resource.id, resource.resource.id);
                }
              } else {
                res.badRequest();
              }
            } else {
              res.badRequest();
            }
          }
        } else {
          res.methodNotAllowed(Methods.POST);
        }
      }
    } else {
      sendTerminalForState(exchange, resource.state);
    }

  }

  @Override
  protected ResourceResult<DBUpload> fetchResource(HttpServerExchange exchange) {
    var authed = getAuthed(exchange);
    var uid = extractInt(exchange, "upload");

    if (uid != null) {
      var upload = App.database().getUploadById(uid, false);
      if (upload != null) {
        if (States.flagged(upload.state, States.Upload.PRIVATE) && (authed == null || authed.id != upload.owner)) {
          return ResourceResult.unauthorized();
        } else {
          return ResourceResult.OK(upload);
        }
      } else {
        return ResourceResult.notFound();
      }
    } else {
      return ResourceResult.notFound();
    }
  }
}
