package com.mtinge.yuugure.services.http.api;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.States;
import com.mtinge.yuugure.data.http.ReportResponse;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.data.postgres.states.UploadState;
import com.mtinge.yuugure.services.database.Database;
import com.mtinge.yuugure.services.database.UploadFetchParams;
import com.mtinge.yuugure.services.database.props.BookmarkProps;
import com.mtinge.yuugure.services.database.props.VoteProps;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
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

import java.sql.Timestamp;
import java.time.Instant;

public class UploadResource extends APIResource<DBUpload> {
  private static final Logger logger = LoggerFactory.getLogger(UploadResource.class);

  @Override
  public PathTemplateHandler getRoutes() {
    return Handlers.pathTemplate()
      .add("/index", this::fetchForIndex)
      .add("/{upload}", this::handleRaw)
      .add("/{upload}/{action}", this::handleAction);
  }

  private void fetchForIndex(HttpServerExchange exchange) {
    if (MethodValidator.handleMethodValidation(exchange, Methods.GET)) {
      var authed = getAuthed(exchange);
      var uploads = App.database().jdbi().withHandle(handle -> App.database().uploads.getIndexUploads(authed, handle));

      Responder.with(exchange).json(Response.good(uploads));
    }
  }

  private void handleRaw(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    if (MethodValidator.handleMethodValidation(exchange, Methods.GET, Methods.DELETE)) {
      var resource = fetchResource(exchange);
      if (resource.state == FetchState.OK) {
        if (exchange.getRequestMethod().equals(Methods.GET)) {
          var renderable = App.database().jdbi().withHandle(handle -> App.database().uploads.makeUploadRenderable(resource.resource, exchange.getAttachment(SessionHandler.ATTACHMENT_KEY), handle));
          res.json(Response.good(renderable));
        } else if (exchange.getRequestMethod().equals(Methods.DELETE)) {
          var authed = getAuthed(exchange);

          // allow deletion if the authenticated user is the resource owner or if they are a mod/admin.
          boolean canDel = authed != null && (authed.id == resource.resource.owner || authed.hasModPerms());
          if (authed == null) {
            res.status(StatusCodes.UNAUTHORIZED).json(Response.fromCode(StatusCodes.UNAUTHORIZED).addMessage("You must be logged in to perform this action."));
          } else if (!canDel) {
            res.status(StatusCodes.FORBIDDEN).json(Response.fromCode(StatusCodes.FORBIDDEN).addMessage("You do not have permission to perform this action."));
          } else {
            boolean adminAction = authed.id != resource.resource.owner;
            String reason = null;
            if (adminAction) {
              reason = extractForm(exchange, "reason");
              if (reason.isBlank()) {
                res.status(StatusCodes.BAD_REQUEST).json(Response.fromCode(StatusCodes.BAD_REQUEST).addMessage("Reason is required."));
                return;
              }
            }

            // finalizing for lambada usage
            final String _reason = reason;
            var delres = App.database().jdbi().inTransaction(handle -> {
              var _ret = App.database().uploads.delete(resource.resource.id, handle);

              if (_ret.isSuccess() && adminAction) {
                App.database().audits.trackAction(authed, resource.resource, "delete", "Admin supplied reason: " + _reason, handle);
              }

              return _ret;
            });

            if (delres.isSuccess()) {
              App.webServer().lobby().in(resource.resource).broadcast(
                OutgoingPacket.uploadStateUpdate(UploadState.fromDb(delres.getResource()))
              );
              App.webServer().lobby().in("account:" + resource.resource.owner).broadcast(
                OutgoingPacket.prepare("remove_upload").addData("id", resource.resource.id)
              );
              res.json(Response.good());
            } else {
              res.status(StatusCodes.INTERNAL_SERVER_ERROR).json(Response.fromCode(StatusCodes.INTERNAL_SERVER_ERROR));
            }
          }
        }
      } else {
        sendTerminalForState(exchange, resource.state);
      }
    }
  }

  private void handleAction(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var action = extract(exchange, "action");
    if (action.isBlank()) {
      this.handleRaw(exchange);
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
                  var report = App.database().jdbi().inTransaction(handle -> App.database().reports.create(resource.resource, authed, frmReason.getValue(), handle));
                  if (report.isSuccess()) {
                    res.json(Response.good(ReportResponse.fromDb(report.getResource())));
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
                var props = new BookmarkProps().active(!exchange.getRequestMethod().equals(Methods.DELETE)).isPublic(!_isPrivate).timestamp(Timestamp.from(Instant.now()));
                var bookmarkResult = App.database().bookmarks.handleFlip(authed, resource.resource, props, handle);
                handle.commit();

                if (bookmarkResult.updated) {
                  var packet = OutgoingPacket.prepare("bookmarks_updated");

                  // send updated state to the websocket for client updates.
                  // to send: bookmark_removed | bookmark_added
                  if (bookmarkResult.isPublic) {
                    if (bookmarkResult.isActive && bookmarkResult.wasActive && !bookmarkResult.wasPublic) {
                      // user changed their bookmark from private to public
                      //  ->bookmark_added
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("change", "add"));
                    } else if (bookmarkResult.isActive && !bookmarkResult.wasActive) {
                      // user added a bookmark
                      //  ->bookmark_added
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("change", "add"));
                    } else {
                      // user removed their bookmark
                      //  ws->bookmark_removed
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("change", "remove"));
                    }
                  } else { // else: bookmark is private
                    if (bookmarkResult.isActive && bookmarkResult.wasActive && bookmarkResult.wasPublic) {
                      // user changed their bookmark from public to private
                      //  ws->bookmark_removed
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("change", "remove"));
                    } // else: the bookmark is either currently inactive or was previously inactive,
                    //         and is currently private. there is no state change to
                    //         report because there was no state known to begin with.
                  }
                }

                return bookmarkResult.updated;
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
                var props = new VoteProps().active(!exchange.getRequestMethod().equals(Methods.DELETE)).isUp(isUpvote);
                var voteRes = App.database().votes.handleFlip(authed, resource.resource, props, handle);
                handle.commit();

                if (voteRes.updated) {
                  var packet = OutgoingPacket.prepare("votes_updated");

                  // Send updated state to the websocket for client updates.
                  // to send: ('swapped'|'added'|'removed', 'upvote'|'downvote')
                  if (voteRes.isActive && voteRes.wasActive) {
                    if (voteRes.isUpvote != voteRes.wasUpvote) {
                      if (voteRes.isUpvote) {
                        // user swapped their vote from a downvote to an upvote
                        //  ws->('swapped', 'upvote')
                        App.webServer().lobby().in(resource.resource).broadcast(packet.addData("action", "swap").addData("upvote", true));
                      } else {
                        // user swapped their vote from an upvote to a downvote
                        //  ws->('swapped', 'downvote')
                        App.webServer().lobby().in(resource.resource).broadcast(packet.addData("action", "swap").addData("upvote", false));
                      }
                    }
                  } else if (voteRes.isActive) {
                    // user re-activated their vote
                    if (voteRes.isUpvote) {
                      // user sent an upvote
                      //  ws->('added', 'upvote')
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("action", "add").addData("upvote", true));
                    } else {
                      // user sent a downvote
                      //  ws->('added', 'downvote')
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("action", "add").addData("upvote", false));
                    }
                  } else if (voteRes.wasActive) {
                    // user removed a vote
                    if (voteRes.isUpvote) {
                      // user removed an upvote
                      //  ws->('removed', 'upvote')
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("action", "remove").addData("upvote", true));
                    } else {
                      // user removed a downvote
                      //  ws->('removed', 'downvote')
                      App.webServer().lobby().in(resource.resource).broadcast(packet.addData("action", "remove").addData("upvote", false));
                    }
                  }
                }

                return voteRes.updated;
              } catch (Exception e) {
                logger.error("Failed to ensure vote for user {} on upload {}.", authed.id, resource.resource.id, e);
                handle.rollback();
                return false;
              }
            });
            res.json(Response.good().addData(modified));
          }
        }
        case "private" -> {
          if (MethodValidator.handleMethodValidation(exchange, Methods.PATCH)) {
            // default this to true by requiring an explicit "false" on the form.
            var isPrivate = !extractForm(exchange, "private").equalsIgnoreCase("false");
            if (authed.id == resource.resource.owner || authed.hasAdminPerms()) {
              boolean adminAction = authed.id != resource.resource.owner;

              var updated = App.database().jdbi().withHandle(handle -> {
                try {
                  handle.begin();

                  // since we're playing with state we might as well lock.
                  handle.execute("SELECT 1 FROM upload WHERE id = ? FOR UPDATE", resource.resource.id);

                  long newState = isPrivate ? States.addFlag(resource.resource.state, States.Upload.PRIVATE) : States.removeFlag(resource.resource.state, States.Upload.PRIVATE);
                  var altered = Database.firstOrNull(
                    handle.createQuery("UPDATE upload SET state = :state WHERE id = :id RETURNING *")
                      .bind("state", newState)
                      .bind("id", resource.resource.id),
                    DBUpload.class
                  );

                  if (adminAction) {
                    App.database().audits.trackAction(authed, resource.resource, "private", "Forced private state to " + isPrivate, handle);
                  }

                  handle.commit();
                  return altered;
                } catch (Exception e) {
                  logger.error("Failed to update private state to {} on upload {}.", isPrivate, resource.resource.id, e);
                  handle.rollback();
                }
                return null;
              });

              if (updated != null) {
                App.webServer().lobby().in(resource.resource).broadcast(
                  OutgoingPacket.uploadStateUpdate(UploadState.fromDb(updated))
                );
                if (isPrivate) {
                  App.webServer().lobby().in("account:" + resource.resource.owner).broadcast(
                    OutgoingPacket.prepare("remove_upload").addData("id", resource.resource.id),
                    channel -> channel.accountId() == null || channel.accountId() != resource.resource.owner
                  );
                } // else: we could send the "upload" packet but we'd have to make it renderable,
                //         not worth the db hit right now. in the future we can check if anyone is
                //         subscribed to the room and if so then take the hit.
                res.json(Response.good());
              } else {
                res.status(StatusCodes.INTERNAL_SERVER_ERROR).json(Response.fromCode(StatusCodes.INTERNAL_SERVER_ERROR));
              }
            } else {
              res.status(StatusCodes.FORBIDDEN).json(Response.fromCode(StatusCodes.FORBIDDEN));
            }
          }
        }
      }
    } else {
      sendTerminalForState(exchange, resource.state);
    }
  }

  @Override
  protected ResourceResult<DBUpload> fetchResource(HttpServerExchange exchange) {
    return getUpload(exchange, "upload");
  }


  public static ResourceResult<DBUpload> getUpload(HttpServerExchange exchange, String paramName) {
    var authed = getAuthed(exchange);
    var uid = extractInt(exchange, paramName);

    if (uid != null) {
      var upload = App.database().jdbi().withHandle(handle -> App.database().uploads.read(uid, new UploadFetchParams(false, true), handle));
      if (upload != null) {
        if (States.flagged(upload.state, States.Upload.PRIVATE) && (authed == null || (!authed.hasModPerms() && authed.id != upload.owner))) {
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
