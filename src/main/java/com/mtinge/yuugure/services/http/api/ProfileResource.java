package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.ProfileResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.SafeAccount;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.database.UploadFetchParams;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.util.MethodValidator;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileResource extends APIResource<DBAccount> {
  private static final Logger logger = LoggerFactory.getLogger(ProfileResource.class);

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
      res.json(Response.good(new ProfileResponse(authed != null && authed.id == resource.resource.id, SafeAccount.fromDb(resource.resource))));
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
        if (MethodValidator.handleMethodValidation(exchange, Methods.GET, Methods.DELETE)) {
          if (exchange.getRequestMethod().equals(Methods.GET)) {
            res.json(Response.good(App.database().getRenderableUploadsForAccount(resource.resource.id, new UploadFetchParams(false, authed != null && authed.id == resource.resource.id))));
          } else if (exchange.getRequestMethod().equals(Methods.DELETE)) {
            if (authed.id != resource.resource.id) {
              res.json(Response.fromCode(StatusCodes.FORBIDDEN));
            } else {
              var token = extractConfirmationToken(exchange);
              if (token != null) {
                if (App.redis().confirmToken(token, authed, true)) {
                  try {
                    var updated = App.database().jdbi().inTransaction(handle ->
                      handle.createUpdate("UPDATE upload SET state = (state | :delete_state) WHERE owner = :owner AND (state & :delete_state) = 0")
                        .bind("owner", authed.id)
                        .bind("delete_state", States.Upload.DELETED)
                        .execute()
                    );
                    res.json(Response.good().addMessage("Deleted " + updated + " uploads."));
                  } catch (Exception e) {
                    logger.error("Failed to delete uploads for account {}.", authed.id, e);
                    res.json(Response.fromCode(StatusCodes.INTERNAL_SERVER_ERROR));
                  }
                } else {
                  res.json(Response.fromCode(StatusCodes.UNAUTHORIZED).addMessage("Invalid confirmation token."));
                }
              } else {
                res.json(Response.fromCode(StatusCodes.UNAUTHORIZED).addMessage("Missing confirmation token"));
              }
            }
          }
        }
      }
    } else {
      sendTerminalForState(exchange, resource.state);
    }

    res.end();
  }

  @Override
  protected ResourceResult<DBAccount> fetchResource(HttpServerExchange exchange) {
    return AccountResource.FetchAccountMeAware(exchange);
  }
}
