package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.Utils;
import com.mtinge.yuugure.data.http.Response;
import com.mtinge.yuugure.data.http.UploadResult;
import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.SessionHandler;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class RouteUpload extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteUpload.class);
  private static RouteUpload instance;
  private static final Object _lock = new Object();
  private static final AtomicLong al = new AtomicLong();

  private PathHandler pathHandler;
  private AutoDetectParser tikaParser;
  private Pattern validMimesPattern;

  private Path tempPath;
  private Path finalPath;

  @SneakyThrows
  public RouteUpload() {
    this.pathHandler = Handlers.path().addExactPath("/", this::upload);
    this.tikaParser = new AutoDetectParser();
    this.validMimesPattern = Pattern.compile(App.config().upload.validMimesPattern, Pattern.CASE_INSENSITIVE);

    this.tempPath = Path.of(App.config().upload.tempDir).toFile().getCanonicalFile().toPath();
    this.finalPath = Path.of(App.config().upload.finalDir).toFile().getCanonicalFile().toPath();
  }

  private void upload(HttpServerExchange exchange) {
    var resp = Responder.with(exchange);
    var account = exchange.getAttachment(SessionHandler.ATTACHMENT_KEY);
    var isAuthed = account != null;
    if (!isAuthed) {
      resp.notAuthorized();
    } else {
      if (validateMethods(exchange, Methods.GET, Methods.POST)) {
        if (exchange.getRequestMethod().equals(Methods.GET)) {
          resp.view("app");
        } else {
          // ensure we have a file
          final UploadResult uploadResult = new UploadResult();
          var form = exchange.getAttachment(FormDataParser.FORM_DATA);
          if (form != null) {
            var frmFile = form.getFirst("file");
            if (frmFile == null || !frmFile.isFileItem()) {
              uploadResult.addError("File is missing from the request.");
            } else {
              // ensure file is of a valid mime, reading the magic bytes to increase detection
              // accuracy
              MediaType fType = null;
              var file = frmFile.getFileItem();
              try {
                var meta = new Metadata();
                fType = tikaParser.getDetector().detect(file.getInputStream(), meta);
              } catch (Exception e) {
                logger.error("Failed to detect filetype.", e);
              }

              if (fType != null && validMimesPattern.matcher(fType.toString()).find()) {
                final String mime = fType.toString();

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
                  final Path outPath = Path.of(finalPath.toString(), sha256);
                  if (!outPath.toFile().exists()) {
                    file.getFile().toFile().renameTo(outPath.toFile());
                  } // else: this is a duplicate upload

                  // insert the upload into the database
                  var mtx = App.redis().getMutex("ul:" + sha256);
                  try {
                    mtx.acquire();
                    App.database().jdbi().useHandle(h -> {
                      var handle = h.begin();
                      boolean committed = false;

                      try {
                        var media = handle.createQuery("SELECT * FROM media WHERE sha256 = :sha256")
                          .bind("sha256", sha256)
                          .map(DBMedia.Mapper)
                          .findFirst().orElse(null);
                        if (media == null) {
                          media = handle.createQuery("INSERT INTO media (sha256, md5, phash, mime) VALUES (:sha256, :md5, :phash, :mime) RETURNING *")
                            .bind("sha256", sha256)
                            .bind("md5", md5)
                            .bind("phash", "")
                            .bind("mime", mime)
                            .map(DBMedia.Mapper)
                            .findFirst().orElse(null);
                        }
                        if (media != null) {
                          var dupedForOwner = handle.createQuery("SELECT EXISTS (SELECT id FROM upload WHERE owner = :owner AND media = :media) AS \"exists\"")
                            .bind("owner", account)
                            .bind("media", media.id)
                            .map((r, c) -> r.getBoolean("exists"))
                            .findFirst().orElse(false);
                          if (dupedForOwner) {
                            uploadResult.addError("You have already uploaded this media.");
                          } else {
                            var toRet = handle.createQuery("INSERT INTO upload (media, owner, upload_date) VALUES (:media, :owner, now()) RETURNING *")
                              .bind("media", media.id)
                              .bind("owner", account)
                              .map(DBUpload.Mapper)
                              .findFirst().orElse(null);
                            if (toRet != null) {
                              uploadResult.setSuccess(true);
                              uploadResult.setMedia(media);
                              uploadResult.setUpload(toRet);
                              handle.commit();
                              committed = true;
                            } else {
                              uploadResult.addError("An internal server error occurred.");
                            }
                          }
                        } else {
                          logger.error("Failed to create media for upload {}.", sha256);
                        }
                      } catch (Exception e) {
                        handle.rollback();
                      } finally {
                        if (!committed) {
                          handle.rollback();
                        }
                      }
                    });
                  } finally {
                    mtx.release();
                  }

                  // report back to the user
                  if (uploadResult.getUpload() != null) {
                    App.messaging().publish(Messaging.TOPIC_UPLOAD, Map.of(
                      "upload_id", uploadResult.getUpload().id,
                      "media_id", uploadResult.getMedia().id
                    ));
                  }
                } catch (Exception e) {
                  logger.error("Failed to read file for hashing");
                }
              } else {
                uploadResult.addError("Invalid file type uploaded, expected an image or a video.");
              }
            }
          } else {
            uploadResult.addError("No uploads were present on the request.");
          }

          var code = uploadResult.hasErrors() ? StatusCodes.BAD_REQUEST : StatusCodes.OK;
          var status = uploadResult.hasErrors() ? StatusCodes.BAD_REQUEST_STRING : StatusCodes.OK_STRING;
          resp.json(new Response(status, code).addData(UploadResult.class, uploadResult));
        }
      }
    }
  }

  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/upload", pathHandler);
  }
}
