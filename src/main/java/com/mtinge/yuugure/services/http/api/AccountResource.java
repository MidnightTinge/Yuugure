package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.ReportResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.SafeAccount;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.http.Responder;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountResource extends APIResource<DBAccount> {
  private static final Logger logger = LoggerFactory.getLogger(AccountResource.class);

  @Override
  public PathTemplateHandler getRoutes() {
    return Handlers.pathTemplate()
      .add("/{account}", this::handleFetch)
      .add("/{account}/{action}", this::handleAction);
  }

  private void handleFetch(HttpServerExchange exchange) {
    var res = Responder.with(exchange);

    var resource = fetchResource(exchange);
    if (resource.state == FetchState.OK) {
      res.json(SafeAccount.fromDb(resource.resource));
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
      if (exchange.getRequestMethod().equals(Methods.POST)) {
        var body = exchange.getAttachment(FormDataParser.FORM_DATA);
        if (body != null) {
          var frmReason = body.getFirst("reason");
          if (frmReason != null && !frmReason.isFileItem()) {
            var report = App.database().createReport(resource.resource, authed, frmReason.getValue());
            if (report != null) {
              res.json(Response.good().addData(ReportResponse.class, ReportResponse.fromDb(report)));
            } else {
              res.internalServerError();
              logger.error("Report returned from database on account {} from user {} was null.", resource.resource.id, resource.resource.id);
            }
          } else {
            res.badRequest();
          }
        } else {
          res.badRequest();
        }
      } else {
        res.methodNotAllowed(Methods.POST);
      }
    } else {
      sendTerminalForState(exchange, resource.state);
    }
  }

  @Override
  protected ResourceResult<DBAccount> fetchResource(HttpServerExchange exchange) {
    var authed = getAuthed(exchange);
    var aId = extractInt(exchange, "account");
    if (aId != null) {
      var account = App.database().getAccountById(aId, false);
      if (account != null) {
        if (States.flagged(account.state, States.Account.PRIVATE) && (authed == null || authed.id != account.id)) {
          return ResourceResult.unauthorized();
        } else {
          return ResourceResult.OK(account);
        }
      } else {
        return ResourceResult.notFound();
      }
    } else {
      return ResourceResult.notFound();
    }
  }
}
