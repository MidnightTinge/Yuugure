package com.mtinge.yuugure.services.http.routes;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.Utils;
import com.mtinge.yuugure.core.Validators;
import com.mtinge.yuugure.data.http.AuthResponse;
import com.mtinge.yuugure.data.http.AuthStateResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.postgres.DBAccount;
import com.mtinge.yuugure.data.postgres.DBSession;
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
      .addExactPath("/check", this::check);
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
            res.json(Response.bad(StatusCodes.BAD_REQUEST, StatusCodes.BAD_REQUEST_STRING).addData(AuthResponse.class, authRes));
          } else {
            var mtxEmail = App.redis().getMutex("reg:em:" + email);
            var mtxUsername = App.redis().getMutex("reg:un:" + username);
            try {
              // this will not lead to deadlocks - you can only progress to acquiring the username
              // after the email is locked. when both threads attempt to lock the email, only one
              // will pass the gate to acquire the username.
              mtxEmail.acquire();
              mtxUsername.acquire();

              String finalPassword = password;
              String finalUsername = username;
              String finalEmail = email;
              App.database().jdbi().useHandle(handle -> {
                var errored = false; // tracks rollback for our finally{} txn.commit
                var txn = handle.begin();

                try {
                  // we're splitting so the user can know which conflicts
                  var emailExists = txn.createQuery("SELECT EXISTS (SELECT id FROM account WHERE lower(email) = lower(:email)) AS \"exists\"")
                    .bind("email", finalEmail)
                    .map((r, __, ___) -> r.getBoolean("exists"))
                    .findFirst().orElse(null);
                  var usernameExists = txn.createQuery("SELECT EXISTS (SELECT id FROM account WHERE lower(username) = lower(:username)) AS \"exists\"")
                    .bind("username", finalUsername)
                    .map((r, __, ___) -> r.getBoolean("exists"))
                    .findFirst().orElse(null);

                  if (emailExists == null || usernameExists == null) {
                    // an error occurred, we should never see this, but just in case it happens we
                    // don't want the default behavior to be confusing to the end-user (e.g. we
                    // shouldn't say the username is taken/unavailable)
                    authRes.addError("An internal server error occurred while interacting with the database. Please try again later.");
                    logger.error("Registration failed.", new Error("The database did not return a valid boolean during registration preflights."));
                    errored = true;
                    txn.rollback();
                  } else if (emailExists || usernameExists) {
                    if (emailExists) {
                      authRes.addInputError("email", "This email is already in use by another account.");
                    }
                    if (usernameExists) {
                      authRes.addInputError("username", "This username is already in use by another account.");
                    }
                  } else {
                    var hash = BCrypt.withDefaults().hashToString(12, finalPassword.toCharArray());

                    var account = txn.createQuery("INSERT INTO account (username, email, password) VALUES (:username, :email, :hash) RETURNING *")
                      .bind("username", finalUsername)
                      .bind("email", finalEmail)
                      .bind("hash", hash)
                      .map(DBAccount.Mapper)
                      .findFirst().orElse(null);

                    if (account == null) {
                      txn.rollback();
                      errored = true;
                      authRes.addError("An internal server occurred while creating the user. Please try again later.");
                      logger.error("Registration failed.", new Error("The database did not return a valid account after insertion."));
                    } else {
                      // TODO set cookie
                      var token = Utils.token(16);
                      var expires = Instant.now().plus(App.config().http.auth.sessionExpires);
                      var session = handle.createQuery("INSERT INTO sessions (token, account, expires) VALUES (:token, :account, :expires) RETURNING *")
                        .bind("token", token)
                        .bind("account", account.id)
                        .bind("expires", Timestamp.from(expires))
                        .map(DBSession.Mapper)
                        .findFirst().orElse(null);

                      if (session != null) {
                        authRes.authed = true;
                        res.cookie(SessionHandler.makeSessionCookie(token, expires));
                      } else {
                        authRes.authed = false;
                        authRes.addError("Your account was created, but automatic login failed. Please try to login manually.");
                        logger.error("Failed to create session for user {}, query returned a null value.", account.id);
                      }
                    }
                  }
                } catch (Exception e) {
                  errored = true;
                  txn.rollback();
                  logger.error("Registration failed (txn catch).", e);
                  authRes.addError("An internal server occurred while creating the user. Please try again later.");
                } finally {
                  if (!errored) {
                    txn.commit();
                  }
                }
              });

              // all db work is done, send the data back to the user.
              var code = authRes.hasErrors() ? StatusCodes.BAD_REQUEST : StatusCodes.OK;
              var status = authRes.hasErrors() ? StatusCodes.BAD_REQUEST_STRING : StatusCodes.OK_STRING;
              res.status(code).json(new Response(status, code).addData(AuthResponse.class, authRes));
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
              handle.createQuery("SELECT * FROM account WHERE lower(email) = lower(:email)")
                .bind("email", frmEmail.getValue().trim()) // email is never null at this point.
                .map(DBAccount.Mapper)
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
                    .map(DBSession.Mapper)
                    .findFirst().orElse(null)
                );

                if (session != null) {
                  authRes.authed = true;
                  res.cookie(SessionHandler.makeSessionCookie(token, expires));
                } else {
                  authRes.authed = false;
                  authRes.addError("An internal server error occurred while logging you in. Please try again.");
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
        res.status(code).json(new Response(status, code).addData(AuthResponse.class, authRes));
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

    Responder.with(exchange).json(Response.good().addData(AuthStateResponse.class, new AuthStateResponse(account != null, account != null ? account.id : null)));
  }

  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/auth", pathHandler);
  }
}
