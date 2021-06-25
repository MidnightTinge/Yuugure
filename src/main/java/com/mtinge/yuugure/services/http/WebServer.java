package com.mtinge.yuugure.services.http;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import com.mtinge.RateLimit.LimiterFactory;
import com.mtinge.yuugure.App;
import com.mtinge.yuugure.services.IService;
import com.mtinge.yuugure.services.http.handlers.*;
import com.mtinge.yuugure.services.http.routes.RouteAPI;
import com.mtinge.yuugure.services.http.routes.RouteAuth;
import com.mtinge.yuugure.services.http.routes.RouteIndex;
import com.mtinge.yuugure.services.http.routes.RouteUpload;
import com.mtinge.yuugure.services.http.ws.Listener;
import io.prometheus.client.exporter.MetricsServlet;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.form.EagerFormParsingHandler;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Getter
@Accessors(fluent = true)
public class WebServer implements IService {
  private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

  private PanicHandler panicHandler;
  private LimiterFactory limiterFactory;
  private Limiters limiters;
  private Undertow undertow;
  private PebbleEngine pebble;
  private ResourceHandler staticHandler;
  private Listener wsListener;
  private ETagHelper eTagHelper;

  public WebServer() {
    //
  }

  @Override
  public void init() throws Exception {
    var engineBuilder = new PebbleEngine.Builder();
    var notFoundHandler = new NotFoundHandler();

    // Set our loader based on VM arguments - if we have a serveLoc defined, we create a loader that
    // loads from the local filesystem and bypasses cache. otherwise we load from the jar resources
    // and enable caching.
    var serveLoc = System.getProperty("serveLoc");
    Loader<String> loader;
    if (serveLoc != null && !serveLoc.isBlank()) {
      var root = Path.of(serveLoc).toAbsolutePath();
      var tpl = root.resolve("templates/").toFile().getAbsoluteFile();
      var stl = root.resolve("static/").toFile().getAbsoluteFile();

      loader = new FileLoader();

      logger.info("Setting template location to {}", tpl.getAbsolutePath());
      loader.setPrefix(tpl.getAbsolutePath());
      loader.setSuffix(".pebble");

      logger.info("Setting static location to {}", stl.getAbsolutePath());
      this.staticHandler = new ResourceHandler(new FileResourceManager(stl), notFoundHandler);

      engineBuilder.cacheActive(!App.isDebug());
    } else {
      loader = new ClasspathLoader();
      loader.setPrefix("templates/");
      loader.setSuffix(".pebble");

      this.staticHandler = new ResourceHandler(new ClassPathResourceManager(getClass().getClassLoader(), "static/"), notFoundHandler);
    }
    this.staticHandler.setDirectoryListingEnabled(false);

    // Ensure our upload directories exist
    var _tempPath = Path.of(App.config().upload.tempDir);
    var _finPath = Path.of(App.config().upload.finalDir);

    if (!_tempPath.toFile().exists()) {
      Files.createDirectories(_tempPath);
    }
    if (!_finPath.toFile().exists()) {
      Files.createDirectories(_finPath);
    }

    // Create a new form parser that handles UTF-8 input and sets our temp upload path to the
    // correct directory.
    var multiPartDef = new MultiPartParserDefinition(_tempPath);
    multiPartDef.setMaxIndividualFileSize(App.config().upload.maxFileSize);
    multiPartDef.setFileSizeThreshold(App.config().upload.maxFileSize);

    var formParser = new EagerFormParsingHandler(
      FormParserFactory.builder(false)
        .withDefaultCharset("UTF-8")
        .withParsers(List.of(
          multiPartDef,
          new FormEncodedDataDefinition()
        ))
        .build()
    );

    this.panicHandler = new PanicHandler();
    this.limiterFactory = new LimiterFactory(panicHandler.getPanicHandler(), App.redis().jedis());
    this.limiters = new Limiters(this.limiterFactory);

    this.wsListener = new Listener();
    this.eTagHelper = new ETagHelper();

    this.pebble = engineBuilder
      .loader(loader)
      .build();
    var builder = Undertow.builder()
      .addHttpListener(App.config().http.port, App.config().http.host)
      .setHandler(
        new RootHandler(
          new AddressHandler(
            panicHandler.wrap(
              new QueryHandler(
                new AcceptsHandler(
                  formParser.setNext(
                    new FormMethodHandler(
                      new SessionHandler(
                        new RouteUpload().wrap(
                          new RouteAuth().wrap(
                            new RouteAPI().wrap(
                              // IMPORTANT: Keep index last, it adds a prefixPath for `/` to handle
                              // static files
                              new RouteIndex().wrap(
                                Handlers.path()
                              )
                            )
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      );

    if (App.config().prometheus.enabled) {
      // Register the Prometheus metrics endpoint via servlet deployment
      var manager = Servlets.defaultContainer().addDeployment(
        Servlets.deployment()
          .setClassLoader(WebServer.class.getClassLoader())
          .setContextPath("")
          .setDeploymentName("")
          .addServlets(
            Servlets.servlet("MyServlet", MetricsServlet.class)
              .addMapping("/")
          )
      );
      manager.deploy();
      var handler = Handlers.path().addPrefixPath("/", manager.start());
      builder.addHttpListener(App.config().prometheus.port, Objects.requireNonNullElse(App.config().prometheus.host, App.config().http.host), handler);
    }

    this.undertow = builder.build();
  }

  @Override
  public void start() throws Exception {
    panicHandler.reload();
    this.undertow.start();

    var _l = (InetSocketAddress) this.undertow.getListenerInfo().get(0).getAddress();
    logger.info("WebServer listening on {}:{}", _l.getAddress().getHostAddress(), _l.getPort());
  }

  @Override
  public void stop() throws Exception {
    this.undertow.stop();
  }
}
