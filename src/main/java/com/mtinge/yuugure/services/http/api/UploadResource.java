package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.ReportResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.postgres.DBUploadBookmark;
import com.mtinge.yuugure.data.postgres.DBUploadVote;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import com.mtinge.yuugure.services.http.util.MethodValidator;
import com.mtinge.yuugure.services.http.ws.packets.OutgoingPacket;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Headers;
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
      var renderable = App.database().makeUploadRenderable(resource.resource, exchange.getAttachment(SessionHandler.ATTACHMENT_KEY));
      res
        .header(Headers.CACHE_CONTROL, "no-store, max-age=0")
        .json(Response.good(renderable));
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
      switch (action.toLowerCase().trim()) {
        case "report" -> {
          if (MethodValidator.handleMethodValidation(exchange, Methods.POST)) {
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
          }
        }
        case "bookmark" -> {
          if (MethodValidator.handleMethodValidation(exchange, Methods.PATCH, Methods.DELETE)) {
            boolean isPrivate = false;

            var qPrivate = exchange.getQueryParameters().get("private");
            if (qPrivate != null && !qPrivate.isEmpty()) {
              isPrivate = qPrivate.getFirst().equalsIgnoreCase("true");
            }

            // for usage in lambda
            final boolean _isPrivate = isPrivate;
            var modified = App.database().jdbi().withHandle(handle -> {
              handle.begin();
              try {
                // lock the existing row for update if it exists
                var existing = handle.createQuery("SELECT * FROM upload_bookmark WHERE account = :account AND upload = :upload FOR UPDATE")
                  .bind("account", authed.id)
                  .bind("upload", resource.resource.id)
                  .map(DBUploadBookmark.Mapper)
                  .findFirst().orElse(null);


                // Create or update an existing bookmark
                var affected = handle.createUpdate("INSERT INTO upload_bookmark (account, upload, active, public) VALUES (:account, :upload, :active, :public) ON CONFLICT ON CONSTRAINT upload_bookmark_pkey DO UPDATE SET account = :account, upload = :upload, active = :active, public = :public")
                  .bind("account", authed.id)
                  .bind("upload", resource.resource.id)
                  .bind("active", !_isPrivate)
                  .bind("public", exchange.getRequestMethod().equals(Methods.PATCH))
                  .execute();
                handle.commit();

                // dev-friendly contextual bools for calculating state changes to report on the
                // websocket...
                boolean isPublic = !_isPrivate;
                boolean wasPublic = existing != null && existing.isPublic;

                boolean isActive = exchange.getRequestMethod().equals(Methods.PATCH);  // Method=PATCH: Create, Method=DELETE: Remove
                boolean wasActive = existing != null && existing.active;

                if (affected > 0) {
                  var packet = OutgoingPacket.prepare("bookmarks_updated");

                  // send updated state to the websocket for client updates.
                  // to send: bookmark_removed | bookmark_added
                  if (isPublic) {
                    if (isActive && wasActive && !wasPublic) {
                      // user changed their bookmark from private to public
                      //  ->bookmark_added
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("change", "add"));
                    } else if (isActive && !wasActive) {
                      // user added a bookmark
                      //  ->bookmark_added
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("change", "add"));
                    } else {
                      // user removed their bookmark
                      //  ws->bookmark_removed
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("change", "remove"));
                    }
                  } else { // else: bookmark is private
                    if (isActive && wasActive && wasPublic) {
                      // user changed their bookmark from public to private
                      //  ws->bookmark_removed
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("change", "remove"));
                    } // else: the bookmark is either currently inactive or was previously inactive,
                    //         and is currently private. there is no state change to
                    //         report because there was no state known to begin with.
                  }
                }

                return affected > 0;
              } catch (Exception e) {
                logger.error("Failed to ensure bookmark for user {} on upload {}.", authed.id, resource.resource.id, e);
                handle.rollback();
                return false;
              }
            });
            res.json(Response.good().addData(modified));
          }
        }
        case "upvote", "downvote" -> {
          boolean isUpvote = action.trim().equalsIgnoreCase("upvote");
          if (MethodValidator.handleMethodValidation(exchange, Methods.PATCH, Methods.DELETE)) {
            var modified = App.database().jdbi().withHandle(handle -> {
              handle.begin();
              try {
                // lock the existing row for update if it exists
                var existing = handle.createQuery("SELECT * FROM upload_vote WHERE account = :account AND upload = :upload FOR UPDATE")
                  .bind("account", authed.id)
                  .bind("upload", resource.resource.id)
                  .map(DBUploadVote.Mapper)
                  .findFirst().orElse(null);

                // dev-friendly contextual bools for calculating state changes to report on the
                // websocket...
                boolean isActive = exchange.getRequestMethod().equals(Methods.PATCH);
                boolean wasActive = existing != null && existing.active;

                boolean wasUpvote = existing != null && existing.isUp;

                int affected = 0;
                if (exchange.getRequestMethod().equals(Methods.PATCH)) {
                  // Create or update an existing vote
                  affected = handle.createUpdate("INSERT INTO upload_vote (account, upload, is_up, active) VALUES (:account, :upload, :is_up, true) ON CONFLICT ON CONSTRAINT upload_vote_pkey DO UPDATE SET account = :account, upload = :upload, is_up = :is_up, active = true")
                    .bind("account", authed.id)
                    .bind("upload", resource.resource.id)
                    .bind("is_up", isUpvote)
                    .execute();
                } else if (exchange.getRequestMethod().equals(Methods.DELETE)) {
                  // Delete the vote if it exists
                  if (existing != null) {
                    affected = handle.createUpdate("UPDATE upload_vote SET active = false WHERE account = :account AND upload = :upload")
                      .bind("account", authed.id)
                      .bind("upload", resource.resource.id)
                      .execute();
                  } else {
                    handle.commit();
                    return true;
                  }
                }

                handle.commit();

                if (affected > 0) {
                  var packet = OutgoingPacket.prepare("votes_updated");

                  // Send updated state to the websocket for client updates.
                  // to send: ('swapped'|'added'|'removed', 'upvote'|'downvote')
                  if (isActive && wasActive) {
                    if (isUpvote != wasUpvote) {
                      if (isUpvote) {
                        // user swapped their vote from a downvote to an upvote
                        //  ws->('swapped', 'upvote')
                        App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("action", "swap").addData("upvote", true));
                      } else {
                        // user swapped their vote from an upvote to a downvote
                        //  ws->('swapped', 'downvote')
                        App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("action", "swap").addData("upvote", false));
                      }
                    }
                  } else if (isActive) {
                    // user re-activated their vote
                    if (isUpvote) {
                      // user sent an upvote
                      //  ws->('added', 'upvote')
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("action", "add").addData("upvote", true));
                    } else {
                      // user sent a downvote
                      //  ws->('added', 'downvote')
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("action", "add").addData("upvote", false));
                    }
                  } else if (wasActive) {
                    // user removed a vote
                    if (isUpvote) {
                      // user removed an upvote
                      //  ws->('removed', 'upvote')
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("action", "remove").addData("upvote", true));
                    } else {
                      // user removed a downvote
                      //  ws->('removed', 'downvote')
                      App.webServer().lobby().in("upload:" + resource.resource.id).broadcast(packet.addData("action", "remove").addData("upvote", false));
                    }
                  }
                }

                return affected > 0;
              } catch (Exception e) {
                logger.error("Failed to ensure vote for user {} on upload {}.", authed.id, resource.resource.id, e);
                handle.rollback();
                return false;
              }
            });
            res.json(Response.good().addData(modified));
          }
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
