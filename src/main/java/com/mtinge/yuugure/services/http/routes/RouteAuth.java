package com.mtinge.yuugure.services.http.routes;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.core.Strings;
import com.mtinge.yuugure.core.Utils;
import com.mtinge.yuugure.core.Validators;
import com.mtinge.yuugure.data.http.*;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.DBSession;
import com.mtinge.yuugure.services.database.props.AccountProps;
import com.mtinge.yuugure.services.database.props.SessionProps;
import com.mtinge.yuugure.services.database.providers.AccountProvider;
import com.mtinge.yuugure.services.database.providers.Provider;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;

public class RouteAuth extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteAuth.class);

  private PathHandler pathHandler;

  public RouteAuth() {
    this.pathHandler = Handlers.path()
      .addExactPath("/login", this::login)
      .addExactPath("/logout", this::logout)
      .addExactPath("/register", this::register)
      .addExactPath("/check", this::check)
      .addExactPath("/confirm", this::confirm);
  }

  private void register(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    if (validateMethods(exchange, Methods.GET, Methods.POST)) {
      if (exchange.getRequestMethod().equals(Methods.GET)) {
        res.view("app");
      } else {
        var authRes = new AuthResponse();
        var form = exchange.getAttachment(FormDataParser.FORM_DATA);

        if (form == null) {
          res.badRequest();
        } else {
          var frmEmail = form.getFirst("email");
          var frmUsername = form.getFirst("username");
          var frmPassword = form.getFirst("password");
          var frmRepeat = form.getFirst("repeat");

          String email = "";
          String username = "";
          String password = "";
          String repeat = "";

          // validate inputs and assign to variables
          if (frmEmail == null || frmEmail.getValue() == null) {
            authRes.addInputError("email", "Missing/invalid email.");
          } else if (!Validators.Email.validEmail(frmEmail.getValue())) {
            authRes.addInputError("email", "Invalid email.");
          } else {
            email = frmEmail.getValue().trim();
          }
          if (frmUsername == null || frmUsername.getValue() == null) {
            authRes.addInputError("username", "Missing/invalid username.");
          } else {
            var _valid = true;
            var validator = new Validators.Text(frmUsername.getValue().trim());
            if (!validator.length(3, 30)) {
              authRes.addInputError("username", "Username length must be between 3 and 30 characters.");
              _valid = false;
            }
            if (!frmUsername.getValue().trim().matches("^[\\p{LD}-._]+$")) {
              authRes.addInputError("username", "Invalid characters in username.");
            }

            if (_valid) {
              username = frmUsername.getValue().trim();
            }
          }
          if (frmPassword == null || frmPassword.getValue() == null) {
            authRes.addInputError("password", "Missing/invalid password.");
          } else {
            password = frmPassword.getValue().trim();
          }
          if (frmRepeat == null || frmRepeat.getValue() == null) {
            authRes.addInputError("repeat", "Missing/invalid repeated password.");
          } else {
            repeat = frmRepeat.getValue().trim();
          }
          if (!authRes.hasErrors() && !password.equals(repeat)) {
            authRes.addInputError("password", "Passwords do not match.");
            authRes.addInputError("repeat", "Passwords do not match.");
          }

          // report errors if necessary, or start the registration process
          if (authRes.hasErrors()) {
            res.json(Response.fromCode(StatusCodes.BAD_REQUEST).addData(authRes));
          } else {
            var mtxEmail = App.redis().getMutex("reg:em:" + email);
            var mtxUsername = App.redis().getMutex("reg:un:" + username);
            try {
              // this will not lead to deadlocks - you can only progress to acquiring the username
              // after the email is locked. when both threads attempt to lock the email, only one
              // will pass the gate to acquire the username.
              mtxEmail.acquire();
              mtxUsername.acquire();

              var accountProps = new AccountProps(email, username, password, 0L, 0L);
              App.database().jdbi().useHandle(handle -> {
                handle.begin();

                var result = App.database().accounts.create(accountProps, handle);
                if (result.isSuccess() && result.getResource() != null) {
                  var sessionProps = new SessionProps(Utils.token(16), Instant.now().plus(App.config().http.auth.sessionExpires), result.getResource().id);
                  var session = App.database().sessions.create(sessionProps, handle);

                  if (session.isSuccess() && session.getResource() != null) {
                    res.cookie(SessionHandler.makeSessionCookie(session.getResource().token, session.getResource().expires.toInstant()));
                    authRes.setAuthed(true);
                  } else {
                    authRes.setAuthed(false);
                    authRes.addError("Your account was created but we failed to log you in automatically. Please try logging in manually.");
                  }

                  // Session failing to insert is not a reason to rollback an account creation. If
                  // we're to this point, all the user's data is inserted and valid, they should be
                  // able to auth manually.
                  handle.commit();
                } else {
                  handle.rollback();
                  authRes.setAuthed(false);

                  if (result.getFailCode() != null) {
                    switch (result.getFailCode()) {
                      case Provider.FAIL_SQL, Provider.FAIL_UNKNOWN -> result.getErrors().forEach(authRes::addError);
                      case AccountProvider.FAIL_EXISTING_EMAIL -> authRes.addInputError("email", "This email is already in use by another account.");
                      case AccountProvider.FAIL_EXISTING_USERNAME -> authRes.addInputError("username", "This username is already in use by another account.");
                    }
                  } else {
                    authRes.addError(Strings.Generic.INTERNAL_SERVER_ERROR);
                  }
                }
              });

              // all db work is done, send the data back to the user.
              var code = authRes.hasErrors() ? StatusCodes.BAD_REQUEST : StatusCodes.OK;
              var status = authRes.hasErrors() ? StatusCodes.BAD_REQUEST_STRING : StatusCodes.OK_STRING;
              res.status(code).json(new Response(status, code, authRes));
            } finally {
              mtxEmail.release();
              mtxUsername.release();
            }
          }
        }
      }
    }
  }

  private void login(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    if (validateMethods(exchange, Methods.GET, Methods.POST)) {
      if (exchange.getRequestMethod().equals(Methods.GET)) {
        res.view("app");
      } else {
        var authRes = new AuthResponse();
        var form = exchange.getAttachment(FormDataParser.FORM_DATA);

        if (form == null) {
          res.badRequest();
        } else {
          var frmEmail = form.getFirst("email");
          var frmPassword = form.getFirst("password");

          if (frmEmail == null || frmEmail.getValue().isBlank()) {
            authRes.addInputError("email", "This field is required.");
          } else if (!Validators.Email.validEmail(frmEmail.getValue())) {
            authRes.addInputError("email", "Invalid email format.");
          }
          if (frmPassword == null || frmPassword.getValue().isBlank()) {
            authRes.addInputError("password", "This field is required.");
          }

          if (!authRes.hasErrors()) {
            var user = App.database().jdbi().withHandle(handle ->
              handle.createQuery("SELECT * FROM account WHERE lower(email) = lower(:email) AND (state & :bad_state) = 0")
                .bind("email", frmEmail.getValue().trim()) // email is never null at this point.
                .bind("bad_state", States.compute(States.Account.DELETED, States.Account.DEACTIVATED))
                .mapTo(DBAccount.class)
                .findFirst().orElse(null)
            );
            if (user != null) {
              if (BCrypt.verifyer().verify(frmPassword.getValue().trim().toCharArray(), user.password.toCharArray()).verified) {
                // TODO: TOTP

                // user is verified, create a session and set the cookie.
                final String token = Utils.token(16);
                final Instant expires = Instant.now().plus(App.config().http.auth.sessionExpires);
                var session = App.database().jdbi().withHandle(handle ->
                  handle.createQuery("INSERT INTO sessions (token, account, expires) VALUES (:token, :account, :expires) RETURNING *")
                    .bind("token", token)
                    .bind("account", user.id)
                    .bind("expires", Timestamp.from(expires))
                    .mapTo(DBSession.class)
                    .findFirst().orElse(null)
                );

                if (session != null) {
                  authRes.authed = true;
                  res.cookie(SessionHandler.makeSessionCookie(token, expires));
                } else {
                  authRes.authed = false;
                  authRes.addError(Strings.Generic.INTERNAL_SERVER_ERROR_RELOAD);
                  logger.error("Failed to create session for user {}, query returned a null value.", user.id);
                }
              } else {
                authRes.addError("Invalid username/password");
              }
            } else {
              authRes.addError("Invalid username/password");
            }
          }
        }

        // all db work is done, send the data back to the user.
        var code = authRes.hasErrors() ? StatusCodes.BAD_REQUEST : StatusCodes.OK;
        var status = authRes.hasErrors() ? StatusCodes.BAD_REQUEST_STRING : StatusCodes.OK_STRING;
        res.status(code).json(new Response(status, code, authRes));
      }
    }
  }

  private void logout(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    res.cookie(SessionHandler.makeSessionCookie("bye", Instant.now().minusSeconds(3600)));
    if (res.wantsJson()) {
      res.json(Response.good());
    } else {
      res.redirect("/");
    }
  }

  private void check(HttpServerExchange exchange) {
    var account = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    var code = account == null ? StatusCodes.UNAUTHORIZED : StatusCodes.OK;

    Responder.with(exchange).status(code).json(Response.fromCode(code).addData(new AuthStateResponse(account != null, SafeAccount.fromDb(account))));
  }

  private void confirm(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var authed = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    if (authed == null) {
      res.json(Response.fromCode(StatusCodes.UNAUTHORIZED));
      return;
    }

    var form = exchange.getAttachment(FormDataParser.FORM_DATA);
    if (form != null) {
      if (form.contains("password")) {
        var password = form.getFirst("password").getValue();
        var authRes = new AuthConfirmResponse();
        if (password.isBlank()) {
          authRes.addInputError("password", "This field is required.");
        } else {
          var verified = BCrypt.verifyer().verify(password.getBytes(StandardCharsets.UTF_8), authed.password.getBytes(StandardCharsets.UTF_8)).verified;
          authRes.setAuthenticated(verified);

          if (verified) {
            authRes.setConfirmationToken(App.redis().getConfirmToken(authed));
          } else {
            authRes.addInputError("password", "Incorrect Password");
          }
        }

        res.json(Response.good(authRes));
      } else {
        res.json(Response.fromCode(StatusCodes.BAD_REQUEST).addMessage("Missing password."));
      }
    } else {
      res.json(Response.fromCode(StatusCodes.BAD_REQUEST).addMessage("No actionable form data"));
    }

    res.end();
  }

  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/auth", pathHandler);
  }
}
