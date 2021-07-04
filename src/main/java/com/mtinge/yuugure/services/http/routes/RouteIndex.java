package com.mtinge.yuugure.services.http.routes;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.postgres.DBMedia;
import com.mtinge.yuugure.data.postgres.DBUpload;
import com.mtinge.yuugure.services.database.UploadFetchParams;
import com.mtinge.yuugure.services.http.Responder;
import com.mtinge.yuugure.services.http.handlers.ViewHandler;
import io.undertow.Handlers;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.zip.CRC32;

public class RouteIndex extends Route {
  private static final Logger logger = LoggerFactory.getLogger(RouteIndex.class);
  private static final AttachmentKey<DBUpload> ATTACH_UPLOAD = AttachmentKey.create(DBUpload.class);

  private final String PROCESSING_ETAG;
  private final PathHandler pathHandler;
  private final ResourceHandler resourceHandler;

  public RouteIndex() {
    this.resourceHandler = new ResourceHandler(PathResourceManager.builder()
      .setBase(Path.of(App.config().upload.finalDir))
      .setETagFunction(App.webServer().eTagHelper())
      .setFollowLinks(false)
      .setCaseSensitive(true)
      .build());
    this.resourceHandler.setCacheTime(Long.valueOf(Duration.ofHours(12).toSeconds()).intValue());
    this.resourceHandler.setDirectoryListingEnabled(false);

    try {
      try (var is = App.class.getResourceAsStream("/processing.png")) {
        if (is == null) {
          throw new IllegalStateException("Failed to generate PROCESSING_ETAG. Could not secure an InputStream.");
        }

        var crc32 = new CRC32();
        byte[] chunk = new byte[8192];
        int read;

        while ((read = is.read(chunk)) > 0) {
          crc32.update(chunk, 0, read);
        }

        PROCESSING_ETAG = "defproc;" + Long.toHexString(crc32.getValue());
      }
    } catch (Exception e) {
      throw new Error("Failed to generate PROCESSING_ETAG.", e);
    }

    this.pathHandler = Handlers.path()
      .addExactPath("/", this::index)
      .addExactPath("/ws", Handlers.websocket(App.webServer().wsListener()::newConnection))
      .addPrefixPath("/leaving", this::handleExternalLink)
      .addPrefixPath("/search", new ViewHandler("app"))
      .addPrefixPath("/dbg", new ViewHandler("app"))
      .addPrefixPath("/view", Handlers.pathTemplate().add("/{id}", this::renderView))
      .addPrefixPath("/full", Handlers.pathTemplate().add("/{id}", this::serveFull))
      .addPrefixPath("/thumb", Handlers.pathTemplate().add("/{id}", this::serveThumb))
      .addPrefixPath("/profile", new ViewHandler("app")) // our profile
      .addPrefixPath("/user", new ViewHandler("app")) // someone else's profile
      .addPrefixPath("/", App.webServer().staticHandler());
  }

  private void _attachUpload(HttpServerExchange exchange) {
    var matches = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
    if (matches != null) {
      var uploadId = matches.getParameters().get("id");
      if (uploadId != null && uploadId.matches("^[0-9]+$")) {
        exchange.putAttachment(ATTACH_UPLOAD, App.database().getUploadById(Integer.parseInt(uploadId), new UploadFetchParams(false, true)));
      }
    }
  }

  private void serveFull(HttpServerExchange exchange) throws Exception {
    _attachUpload(exchange);
    var upload = exchange.getAttachment(ATTACH_UPLOAD);
    if (upload != null) {
      var media = App.database().getMediaById(upload.media);
      if (media != null) {
        _serveFromUploadsDir(exchange, media.sha256 + ".full", media.mime);
      }
    }
  }

  private void serveThumb(HttpServerExchange exchange) throws Exception {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this::serveThumb);
      return;
    }

    _attachUpload(exchange);
    var upload = exchange.getAttachment(ATTACH_UPLOAD);
    if (upload != null) {
      var media = App.database().getMediaById(upload.media);
      if (media != null) {
        var path = Path.of(App.config().upload.finalDir, media.sha256 + ".thumb");
        if (path.toFile().exists()) {
          // serve from the filesystem
          _serveFromUploadsDir(exchange, media.sha256 + ".thumb", "image/png"); // all thumbs are image/png
        } else {
          // have to pump the defualt processing png manually
          exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "image/png");
          exchange.getResponseHeaders().put(Headers.ETAG, PROCESSING_ETAG);
          exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "public, max-age=300");
          try (var is = App.class.getResourceAsStream("/processing.png")) {
            if (is != null) {
              exchange.startBlocking();
              byte[] chunk = new byte[8192];
              int read;
              while ((read = is.read(chunk)) > 0) {
                exchange.getOutputStream().write(chunk, 0, read);
                exchange.getOutputStream().flush();
              }
            } else {
              // nothing else to do, our fallback png failed to pump. just end the exchange
              exchange.setStatusCode(StatusCodes.NOT_FOUND).endExchange();
            }
          } finally {
            exchange.getOutputStream().close();
          }
        }
      }
    }
  }

  private void _serveFromUploadsDir(HttpServerExchange exchange, String fileName, String mime) throws Exception {
    // note: I don't like this, but this is the cleanest way to get everything working without
    //       ripping out undertow's underlying code for ourselves. This handles etags, byte ranges,
    //       method validation, directory walking exploits, and everything else we would normally
    //       want to implement ourselves.
    exchange.setRelativePath(fileName);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, mime);
    resourceHandler.handleRequest(exchange);
  }

  private void renderView(HttpServerExchange exchange) {
    if (validateMethods(exchange, Methods.GET)) {
      var res = Responder.with(exchange);

      var match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
      if (match != null && match.getParameters().get("id") != null && match.getParameters().get("id").matches("^[0-9]+$")) {
        var upload = App.database().jdbi().withHandle(handle ->
          handle.createQuery("SELECT * FROM upload WHERE id = CAST(:id AS INT)")
            .bind("id", match.getParameters().get("id"))
            .map(DBUpload.Mapper)
            .findFirst().orElse(null)
        );
        if (upload != null) {
          var meta = App.database().jdbi().withHandle(handle ->
            handle.createQuery("SELECT * FROM media WHERE id = :id")
              .bind("id", upload.media)
              .map(DBMedia.Mapper)
              .first()
          );

          res.view("app", Map.of(
            "meta", Map.of(
              "og:title", "Upload",
              "og:type", meta.mime.startsWith("image/") ? "picture" : "video",
              "og:image", "/thumb/" + upload.id,
              "og:url", "/view/" + upload.id
            )
          ));
        } else {
          res.view("app");
        }
      } else {
        res.view("app");
      }
    }
  }

  private void handleExternalLink(HttpServerExchange exchange) {
    var res = Responder.with(exchange);
    var dqUrl = exchange.getQueryParameters().get("url");
    if (dqUrl == null || dqUrl.isEmpty()) {
      res.redirect("/");
    } else {
      res.view("leaving", Map.of("url", dqUrl.getFirst()));
    }
  }

  @Override
  public PathHandler wrap(PathHandler chain) {
    return chain.addPrefixPath("/", pathHandler);
  }

  private void index(HttpServerExchange exchange) {
    Responder.with(exchange).view("app");
  }
}
