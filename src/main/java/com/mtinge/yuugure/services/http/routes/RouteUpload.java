package com.mtinge.yuugure.services.http.routes;

import com.mtinge.EBMLReader.EBMLReader;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.*;
import com.mtinge.yuugure.core.TagManager.TagCategory;
import com.mtinge.yuugure.core.TagManager.TagDescriptor;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.UploadResult;
import com.mtinge.yuugure.data.postgres.DBTag;
import com.mtinge.yuugure.services.database.props.MediaProps;
import com.mtinge.yuugure.services.database.props.ProcessingQueueProps;
import com.mtinge.yuugure.services.database.props.UploadProps;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.AddressHandler;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
import com.mtinge.yuugure.services.http.ws.packets.OutgoingPacket;
import com.mtinge.yuugure.services.messaging.Messaging;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import lombok.SneakyThrows;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RouteUpload extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteUpload.class);

  private PathHandler pathHandler;
  private AutoDetectParser tikaParser;
  private Pattern validMimesPattern;

  private Path tempPath;
  private Path finalPath;

  private ExecutorService uploadExecutor;

  @SneakyThrows
  public RouteUpload() {
    this.pathHandler = Handlers.path().addExactPath("/", this::upload);
    this.tikaParser = new AutoDetectParser();
    this.validMimesPattern = Pattern.compile(App.config().upload.validMimesPattern, Pattern.CASE_INSENSITIVE);

    this.tempPath = Path.of(App.config().upload.tempDir).toFile().getCanonicalFile().toPath();
    this.finalPath = Path.of(App.config().upload.finalDir).toFile().getCanonicalFile().toPath();

    this.uploadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), ThreadFactories.prefixed("UploadExecutor/"));
  }

  private void upload(HttpServerExchange exchange) {
    var resp = Responder.with(exchange);
    var account = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    var isAuthed = account != null;
    if (!isAuthed) {
      if (resp.wantsJson()) {
        resp.status(StatusCodes.UNAUTHORIZED).json(Response.fromCode(StatusCodes.UNAUTHORIZED));
      } else {
        resp.status(StatusCodes.UNAUTHORIZED).view("app");
      }
    } else {
      if (validateMethods(exchange, Methods.GET, Methods.POST)) {
        if (exchange.getRequestMethod().equals(Methods.GET)) {
          resp.view("app");
        } else {
          var check = App.webServer().limiters().uploadLimiter().check(exchange.getAttachment(AddressHandler.ATTACHMENT_KEY));
          if (check.overLimit) {
            if (check.panicWorthy) {
              // if panicWorthy=true we've already panicked.
              PrometheusMetrics.PANIC_TRIGGERS_TOTAL.inc();
            }
            PrometheusMetrics.RATELIMIT_TRIPS_TOTAL.labels("upload").inc();
            resp.ratelimited(check);
            return;
          }
          if (exchange.isInIoThread()) {
            exchange.dispatch(this.uploadExecutor, this::upload);
            return;
          }

          final UploadResult uploadResult = new UploadResult();
          var form = exchange.getAttachment(FormDataParser.FORM_DATA);
          if (form != null) {
            // check if upload should be marked as private
            var frmPrivate = form.getFirst("private") == null ? "false" : form.getFirst("private").getValue();
            boolean isPrivate = frmPrivate.equalsIgnoreCase("true") || frmPrivate.equalsIgnoreCase("on");

            // Ensure we have tags
            var frmTags = form.getFirst("tags");
            if (frmTags == null || frmTags.getValue().isBlank()) {
              uploadResult.addInputError("tags", "This field is required.");
            } else {
              var tags = Arrays.stream(frmTags.getValue().split(" "))
                .filter(str -> !str.isBlank())
                .map(str -> TagDescriptor.parse(str, true))
                .collect(Collectors.toList());

              // Strip all system tags from the user's posted tags
              var iter = tags.iterator();
              while (iter.hasNext()) {
                var tag = iter.next();
                if (!tag.category.equals(TagCategory.USERLAND)) {
                  iter.remove();
                  uploadResult.addNotice("Refused to accept tag \"" + tag.name + "\" due to it being a system tag.");
                }
              }

              // Ensure we have a file
              var frmFile = form.getFirst("file");
              if (frmFile == null || !frmFile.isFileItem()) {
                uploadResult.addError("File is missing from the request.");
              } else {
                // ensure file is of a valid mime, reading the magic bytes to increase detection
                // accuracy
                MediaType fType = null;
                var file = frmFile.getFileItem();
                try {
                  fType = tikaParser.getDetector().detect(file.getInputStream(), new Metadata());
                } catch (Exception e) {
                  logger.error("Failed to detect filetype.", e);
                }

                if (fType != null) {
                  String _mime = fType.toString();
                  if (_mime.equals("application/x-matroska")) {
                    try {
                      logger.debug("got a matroska upload, extracting webm/mkv...");

                      var is = file.getInputStream();
                      is.mark(9216);
                      byte[] chunk = new byte[8192];
                      is.read(chunk);
                      is.reset();

                      var type = EBMLReader.identify(chunk);
                      logger.debug("extracted: {}", type);
                      if (type != null) {
                        _mime = type;
                      }
                    } catch (Exception e) {
                      logger.error("Failed to extract type from EBML.", e);
                    }
                  }

                  if (validMimesPattern.matcher(_mime).find()) {
                    final String mime = _mime;

                    // move file and calculate hashes
                    try {
                      var dgMD5 = MessageDigest.getInstance("MD5");
                      var dgSHA256 = MessageDigest.getInstance("SHA256");

                      // read in the file for hashing
                      var is = file.getInputStream();
                      byte[] chunk = new byte[8192];
                      int numRead;
                      while ((numRead = is.read(chunk)) != -1) {
                        dgMD5.update(chunk, 0, numRead);
                        dgSHA256.update(chunk, 0, numRead);
                      }

                      var md5 = Utils.toHex(dgMD5.digest());
                      var sha256 = Utils.toHex(dgSHA256.digest());

                      // move upload to the final dir
                      final Path outPath = Path.of(finalPath.toString(), sha256 + ".full");
                      if (!outPath.toFile().exists()) {
                        file.getFile().toFile().renameTo(outPath.toFile());
                      } // else: this is a duplicate upload

                      // insert the upload into the database
                      var mtx = App.redis().getMutex("ul:" + sha256);
                      try {
                        LinkedList<DBTag> dbtags;
                        if (mtx.acquire()) {
                          dbtags = App.database().jdbi().withHandle(h -> {
                            var ret = new LinkedList<DBTag>();

                            var handle = h.begin();
                            try {
                              var ensured = App.tagManager().ensureAll(tags, true, handle);

                              ret.addAll(ensured.tags);
                              ensured.messages.forEach(uploadResult::addNotice);

                              handle.commit();
                            } catch (Exception e) {
                              logger.error("(upload) Failed to ensure tags \"{}\".", tags.stream().map(t -> t.name).collect(Collectors.joining(", ")), e);
                              handle.rollback();
                            }

                            return ret;
                          });

                          if (dbtags == null || dbtags.isEmpty()) {
                            if (uploadResult.getNotices().isEmpty()) {
                              uploadResult.addError("Failed to ensure tags, please try again later.");
                            } // else: we already reported the TagCreationResult messages.
                          } else {
                            var inserted = App.database().jdbi().withHandle(h -> {
                              var handle = h.begin();
                              boolean handled = false;

                              try {
                                var media = App.database().media.readBySha(sha256, handle);
                                if (media == null) {
                                  media = App.database().media.create(new MediaProps(sha256, md5, "", mime), handle).getResource();
                                }
                                if (media != null) {
                                  var dupedForOwner = handle.createQuery("SELECT EXISTS (SELECT id FROM upload WHERE owner = :owner AND media = :media) AS \"exists\"")
                                    .bind("owner", account.id)
                                    .bind("media", media.id)
                                    .map((r, c) -> r.getBoolean("exists"))
                                    .findFirst().orElse(false);
                                  if (dupedForOwner) {
                                    uploadResult.addInputError("file", "You have already uploaded this file. MD5: " + md5 + ". SHA256: " + sha256);
                                  } else {
                                    long uploadState = States.flagged(account.state, States.Account.TRUSTED_UPLOADS) ? 0L : States.Upload.MODERATION_QUEUED;
                                    if (isPrivate) {
                                      uploadState = States.addFlag(uploadState, States.Upload.PRIVATE);
                                    }

                                    var toRet = App.database().uploads.create(new UploadProps().media(media.id).owner(account.id).state(uploadState), handle).getResource();
                                    if (toRet != null) {
                                      var pq = App.database().processors.create(new ProcessingQueueProps().upload(toRet.id), handle);
                                      if (pq != null) {
                                        uploadResult.setSuccess(true);
                                        uploadResult.setUpload(App.database().uploads.makeUploadRenderable(toRet, account, handle));
                                        handle.commit();
                                        handled = true;

                                        App.mediaProcessor().wakeWorkers();
                                        return toRet;
                                      } else {
                                        uploadResult.addError("Failed to insert into the processing queue, the upload has been aborted. Please try again later.");
                                        handle.rollback();
                                      }
                                      handled = true;
                                    } else {
                                      uploadResult.addError(Strings.Generic.INTERNAL_SERVER_ERROR);
                                    }
                                  }
                                } else {
                                  logger.error("Failed to create media for upload {}.", sha256);
                                }
                              } catch (Exception e) {
                                handle.rollback();
                                handled = true;
                                logger.error("Caught an SQL error during upload.", e);
                                uploadResult.addError("Failed to finalize upload. Please wait a minute and try again.");
                              } finally {
                                if (!handled) {
                                  handle.rollback();
                                }
                              }
                              return null;
                            });

                            if (inserted != null) {
                              // We do tagging/search indexing as a separate transaction to ensure
                              // everything has been committed in the database.
                              App.database().jdbi().inTransaction(handle -> App.database().tags.addTagsToUpload(inserted.id, dbtags, handle));
                              App.elastic().newUpload(inserted, dbtags);
                            }
                          }
                        }
                      } finally {
                        mtx.release();
                      }

                      // report back to the user
                      if (uploadResult.getUpload() != null) {
                        PrometheusMetrics.UPL_TOTAL.inc();
                        App.messaging().publish(Messaging.TOPIC_UPLOAD, Map.of(
                          "upload_id", uploadResult.getUpload().upload.id,
                          "media_id", uploadResult.getUpload().media.id
                        ));
                        App.webServer().wsListener().getLobby().in("account:" + uploadResult.getUpload().owner.id).broadcast(OutgoingPacket.prepare("upload").addData("upload", uploadResult.getUpload()));
                      }
                    } catch (Exception e) {
                      logger.error("Failed to read file for hashing", e);
                    }
                  } else {
                    uploadResult.addInputError("file", "Invalid file type uploaded, expected an image or a video.");
                    logger.debug("User tried to upload an invalid mime: {}", _mime);
                  }
                } else {
                  uploadResult.addInputError("file", "Invalid file received, unable to detect if it is an image or video.");
                }
              }
            }
          } else {
            uploadResult.addInputError("file", "This field is required.");
            uploadResult.addError("No uploads were present on the request.");
          }

          var code = uploadResult.hasErrors() ? StatusCodes.BAD_REQUEST : StatusCodes.OK;
          var status = uploadResult.hasErrors() ? StatusCodes.BAD_REQUEST_STRING : StatusCodes.OK_STRING;
          resp.status(code).json(new Response(status, code, uploadResult));
        }
      }
    }
  }

  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/upload", pathHandler);
  }
}
