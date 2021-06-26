package com.mtinge.yuugure.services.http.api;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.core.Validators;
import com.mtinge.yuugure.data.http.AccountUpdateResponse;
import com.mtinge.yuugure.data.http.ReportResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.SafeAccount;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import com.mtinge.yuugure.services.http.util.MethodValidator;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;
import io.undertow.util.PathTemplateMatch;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

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
      if (MethodValidator.handleMethodValidation(exchange, Methods.GET, Methods.DELETE)) {
        if (exchange.getRequestMethod().equals(Methods.GET)) {
          res.json(SafeAccount.fromDb(resource.resource));
        } else if (exchange.getRequestMethod().equals(Methods.DELETE)) {
          var authed = getAuthed(exchange);
          if (authed == null) {
            res.status(StatusCodes.UNAUTHORIZED).json(Response.fromCode(StatusCodes.UNAUTHORIZED));
          } else if (authed.id != resource.resource.id) {
            res.status(StatusCodes.FORBIDDEN).json(Response.fromCode(StatusCodes.FORBIDDEN));
          } else {
            var token = extractConfirmationToken(exchange);
            if (token != null) {
              if (App.redis().confirmToken(token, authed, true)) {
                try {
                  App.database().deleteAccount(authed.id);
                  res.json(Response.good());
                } catch (Exception e) {
                  logger.error("Failed to handle account deletion for ID {}.", authed.id, e);
                  res.status(StatusCodes.INTERNAL_SERVER_ERROR).json(Response.fromCode(StatusCodes.INTERNAL_SERVER_ERROR));
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
      switch (action.toLowerCase()) {
        case "report" -> {
          if (MethodValidator.handleMethodValidation(exchange, Methods.POST)) {
            var body = exchange.getAttachment(FormDataParser.FORM_DATA);
            if (body != null) {
              var frmReason = body.getFirst("reason");
              if (frmReason != null && !frmReason.isFileItem()) {
                var report = App.database().createReport(resource.resource, authed, frmReason.getValue());
                if (report != null) {
                  res.json(Response.good().addData(ReportResponse.fromDb(report)));
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
          }
        }
        case "email" -> {
          if (MethodValidator.handleMethodValidation(exchange, Methods.PATCH)) {
            if (authed.id != resource.resource.id) {
              res.json(Response.fromCode(StatusCodes.FORBIDDEN));
            } else {
              var resp = new AccountUpdateResponse();
              var email = extractForm(exchange, "email");
              var password = extractForm(exchange, "password");

              if (email.isBlank()) {
                resp.addInputError("email", "This field is required.");
              } else if (!Validators.Email.validEmail(email)) {
                resp.addInputError("email", "Invalid email format.");
              }

              if (password.isBlank()) {
                resp.addInputError("password", "This field is required.");
              }

              if (!resp.hasErrors()) {
                var verified = BCrypt.verifyer().verify(password.getBytes(StandardCharsets.UTF_8), authed.password.getBytes(StandardCharsets.UTF_8)).verified;
                if (!verified) {
                  resp.addInputError("password", "Incorrect password.");
                } else {
                  var updated = App.database().updateAccountEmail(resource.resource.id, email);
                  if (!updated) {
                    logger.error("updateAccountEmail returned false");
                    resp.addError("An internal server error occurred.");
                  }
                }
              }

              var code = resp.hasErrors() ? StatusCodes.BAD_REQUEST : StatusCodes.OK;
              var message = resp.hasErrors() ? StatusCodes.BAD_REQUEST_STRING : StatusCodes.OK_STRING;
              res.status(code).json(new Response(message, code, resp));
            }
          }
        }
        case "password" -> {
          if (MethodValidator.handleMethodValidation(exchange, Methods.PATCH)) {
            if (authed.id != resource.resource.id) {
              res.json(Response.fromCode(StatusCodes.FORBIDDEN));
            } else {
              var resp = new AccountUpdateResponse();
              var newPassword = extractForm(exchange, "newPassword");
              var currentPassword = extractForm(exchange, "currentPassword");
              var repeat = extractForm(exchange, "repeat");

              if (newPassword.isBlank()) {
                resp.addInputError("newPassword", "This field is required.");
              }

              if (currentPassword.isBlank()) {
                resp.addInputError("currentPassword", "This field is required.");
              }

              if (repeat.isBlank()) {
                resp.addInputError("current", "This field is required.");
              }

              if (!(newPassword.isBlank() && repeat.isBlank()) && !newPassword.equals(repeat)) {
                resp.addInputError("password", "Passwords do not match.");
                resp.addInputError("repeated", "Passwords do not match.");
              }

              if (!resp.hasErrors()) {
                var verified = BCrypt.verifyer().verify(currentPassword.getBytes(StandardCharsets.UTF_8), authed.password.getBytes(StandardCharsets.UTF_8)).verified;
                if (!verified) {
                  resp.addInputError("current", "Incorrect password.");
                } else {
                  var newHash = BCrypt.withDefaults().hash(12, newPassword.getBytes(StandardCharsets.UTF_8));
                  var updated = App.database().updateAccountPassword(resource.resource.id, new String(newHash));
                  if (!updated) {
                    logger.error("updateAccountPassword returned false");
                    resp.addError("An internal server error occurred.");
                  }
                }
              }

              var code = resp.hasErrors() ? StatusCodes.BAD_REQUEST : StatusCodes.OK;
              var message = resp.hasErrors() ? StatusCodes.BAD_REQUEST_STRING : StatusCodes.OK_STRING;
              res.status(code).json(new Response(message, code, resp));
            }
          }
        }
      }
    } else {
      sendTerminalForState(exchange, resource.state);
    }
  }

  @Override
  protected ResourceResult<DBAccount> fetchResource(HttpServerExchange exchange) {
    return FetchAccountMeAware(exchange);
  }

  public static ResourceResult<DBAccount> FetchAccountMeAware(HttpServerExchange exchange) {
    var authed = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    var params = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    if (params != null) {
      var aId = params.getParameters().getOrDefault("account", "");
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
          var account = App.database().getAccountById(Integer.parseInt(aId), false);
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
    } else {
      return ResourceResult.notFound();
    }
  }
}
