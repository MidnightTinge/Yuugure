package com.mtinge.yuugure.services.http.handlers;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.postgres.DBSession;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.AttachmentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class SessionHandler implements HttpHandler {
  public static final AttachmentKey<Integer> ATTACHMENT_KEY = AttachmentKey.create(Integer.class);

  private static final Logger logger = LoggerFactory.getLogger(SessionHandler.class);
  private static final ConcurrentHashMap<String, Byte> ignoredPaths = new ConcurrentHashMap<>();
  private static final LinkedList<Pattern> ignoredPatterns = new LinkedList<>();

  static {
    ignoredPaths.put("/favicon.ico", Byte.MAX_VALUE);
    ignoredPatterns.add(Pattern.compile("^/css/"));
    ignoredPatterns.add(Pattern.compile("^/js/"));
    ignoredPatterns.add(Pattern.compile("^/webfonts/"));
  }

  private final HttpHandler next;

  public SessionHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    try {
      if (!isIgnored(exchange.getRequestURI())) {
        var sessCookie = exchange.getRequestCookie(App.config().http.auth.cookieName);
        if (sessCookie != null) {
          // check if the session is valid
          var aId = SessionHandler.tokenToAccount(sessCookie.getValue(), true);
          if (aId != null) {
            // touch the session expiry
            final Instant expires = Instant.now().plus(App.config().http.auth.sessionExpires);
            var session = App.database().jdbi().withHandle(handle ->
              handle.createQuery("UPDATE sessions SET expires = :expires WHERE token = :token RETURNING *")
                .bind("token", sessCookie.getValue())
                .bind("expires", expires)
                .map(DBSession.Mapper)
                .findFirst().orElse(null)
            );

            // update the user's cookie expiration
            if (session != null) {
              exchange.setResponseCookie(makeSessionCookie(session.token, session.expires.toInstant()));
            } else {
              logger.error("Failed to update session for user {}, the update query didn't return a value.", aId);
            }

            // attach the account ID to our session
            exchange.putAttachment(ATTACHMENT_KEY, aId);
          }
        }
      }
    } finally {
      next.handleRequest(exchange);
    }
  }

  private boolean isIgnored(String path) {
    if (ignoredPaths.get(path) != null) return true;
    for (Pattern ignoredPattern : ignoredPatterns) {
      if (ignoredPattern.matcher(path).find()) {
        ignoredPaths.put(path, Byte.MAX_VALUE);
        return true;
      }
    }

    return false;
  }

  public static Integer tokenToAccount(String token, boolean purgeIfExpired) {
    return App.database().jdbi().withHandle(handle -> {
      var session = handle.createQuery("SELECT * FROM sessions WHERE token = :token")
        .bind("token", token)
        .map(DBSession.Mapper)
        .findFirst().orElse(null);

      if (session != null) {
        if (Instant.now().isAfter(session.expires.toInstant())) {
          if (purgeIfExpired) {
            try {
              handle.createUpdate("DELETE FROM sessions WHERE token = :token")
                .bind("token", token)
                .execute();
            } catch (Exception e) {
              logger.error("Failed to kill session " + session.id, e);
            }
          }

          return null;
        } else {
          return session.account;
        }
      }

      return null;
    });
  }

  public static Cookie makeSessionCookie(String token, Instant expires) {
    return new CookieImpl(App.config().http.auth.cookieName, token)
      .setExpires(Date.from(expires))
      .setSecure(App.config().http.auth.secure)
      .setDomain(App.config().http.auth.domain)
      .setPath("/")
      .setSameSiteMode("Lax");
  }
}
