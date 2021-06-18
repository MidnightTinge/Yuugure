package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.ProfileResponse;
import com.mtinge.yuugure.data.http.RenderableUpload;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.SafeAccount;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.database.UploadFetchParams;
import com.mtinge.yuugure.services.http.Responder;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;

public class ProfileResource extends APIResource<DBAccount> {
  @Override
  public PathTemplateHandler getRoutes() {
    return Handlers.pathTemplate()
      .add("/{account}", this::handleFetch)
      .add("/{account}/{action}", this::handleAction);
  }

  private void handleFetch(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var authed = getAuthed(exchange);
    var resource = fetchResource(exchange);
    if (resource.state == FetchState.OK) {
      res.json(Response.good().addData(ProfileResponse.class, new ProfileResponse(authed != null && authed.id == resource.resource.id, SafeAccount.fromDb(resource.resource))));
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
    var resource = fetchResource(exchange);

    if (resource.state == FetchState.OK) {
      if (action.equalsIgnoreCase("uploads")) {
        res.json(Response.good().addAll(RenderableUpload.class, App.database().getRenderableUploadsForAccount(resource.resource.id, new UploadFetchParams(false, authed != null && authed.id == resource.resource.id))));
      }
    } else {
      sendTerminalForState(exchange, resource.state);
    }

    res.end();
  }

  @Override
  protected ResourceResult<DBAccount> fetchResource(HttpServerExchange exchange) {
    var authed = getAuthed(exchange);
    var aId = extract(exchange, "account");

    if (!aId.isBlank()) {
      var isNumeric = aId.matches("^[0-9]+$");
      if (aId.equalsIgnoreCase("@me") || (authed != null && isNumeric && aId.equals(String.valueOf(authed.id)))) {
        // requested self
        if (authed == null) {
          return ResourceResult.notFound();
        } else {
          return ResourceResult.OK(authed);
        }
      } else if (isNumeric) {
        // requested someone else
        var account = App.database().getAccountById(Integer.parseInt(aId));
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
    } else {
      return ResourceResult.notFound();
    }
  }
}
