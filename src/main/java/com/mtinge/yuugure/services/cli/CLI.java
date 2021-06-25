package com.mtinge.yuugure.services.cli;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.adapters.DurationAdapter;
import com.mtinge.yuugure.scripts.*;
import com.mtinge.yuugure.services.IService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;

public class CLI implements IService {
  private static final Logger logger = LoggerFactory.getLogger(CLI.class);

  private ScriptFactory scriptFactory;
  private Scanner scanner;

  @Override
  public void init() throws Exception {
    scanner = new Scanner(System.in);
    scriptFactory = new ScriptFactory().register(
      new RenderAllComments(),
      new ReprocessAllUploads(),
      new ReprocessSpecificUpload(),
      new ActiveMediaProcessors()
    );
  }

  @Override
  public void start() throws Exception {
    new Thread(() -> {
      logger.info("Starting CLI loop");
      while (scanner.hasNext()) {
        try {
          var line = scanner.nextLine();
          if (line == null || line.isBlank()) {
            continue;
          }

          var args = new LinkedList<>(Arrays.asList(line.split(" ")));
          var command = args.removeFirst();

          switch (command.toLowerCase()) {
            case "script", "scripts" -> {
              if (args.isEmpty() || args.getFirst().equalsIgnoreCase("list")) {
                var buffer = new StringBuilder();
                buffer.append("Registered Scripts:\n");
                for (var script : scriptFactory.getRegistered()) {
                  buffer.append("\t").append(script.name).append("\n");
                }
                System.out.println(buffer);
              } else {
                var name = args.removeFirst();
                var script = scriptFactory.get(name);
                if (script != null) {
                  System.out.println("Running script " + name + "...");
                  script.run(args);
                }
              }
            }
            case "panic", "panics" -> {
              if (args.isEmpty() || args.getFirst().equalsIgnoreCase("list")) {
                var denials = App.webServer().panicHandler().getDenials(false);
                var entries = denials.entrySet();
                var sb = new StringBuilder();

                sb.append("Denials\n");
                for (var entry : entries) {
                  sb.append("\t").append(entry.getKey()).append(" - Expires: ").append(entry.getValue()).append("\n");
                }
                sb.append("--- Total: ").append(entries.size()).append(" ---");

                System.out.println(sb);
              } else {
                var subcommand = args.removeFirst();
                switch (subcommand.toLowerCase()) {
                  case "add", "put", "block", "deny", "set" -> {
                    // usage: panic deny <IP> <EXPIRY>.
                    // at this point we need at least 2 args in the array.
                    if (args.size() < 2) {
                      logger.error("Usage: {} {} <IP> <EXPIRY>", command, subcommand);
                    } else {
                      var ip = args.removeFirst();
                      var expiry = args.removeFirst();
                      if (expiry.matches("^[0-9]+$")) {
                        // received a long, assume it's the expiry epoch
                        var parsed = Long.parseLong(expiry);
                        App.webServer().panicHandler().deny(ip, parsed);
                        logger.info("Blocked {} until {}", ip, Instant.ofEpochMilli(parsed));
                      } else {
                        // attempt to convert a duration using our custom JSON duration marshaller
                        var duration = DurationAdapter.durationFromString(expiry);
                        if (duration != null && !duration.isNegative() && !duration.isZero()) {
                          var until = Instant.now().plus(duration);
                          App.webServer().panicHandler().deny(ip, until.toEpochMilli());
                          logger.info("Blocked {} until {}", ip, until);
                        } else {
                          logger.error("Invalid expiry provided. Expected either an epoch timestamp or a duration (\"1d\")");
                        }
                      }
                    }
                  }
                  case "remove", "del", "allow" -> {
                    // usage: panic allow <IP>
                    // at this point we need at least 1 arg in the array.
                    if (args.size() < 1) {
                      logger.error("Usage: {} {} <IP>", command, subcommand);
                    } else {
                      var ip = args.removeFirst();
                      var allowed = App.webServer().panicHandler().allow(ip);
                      if (allowed) {
                        logger.info("Unblocked {}", ip);
                      } else {
                        logger.info("Failed to unblock {}, ensure it is actually blocked.", ip);
                      }
                    }
                  }
                }
              }
            }
            case "etc", "etagcache", "etag_cache", "etcache" -> {
              if (args.isEmpty() || args.getFirst().equalsIgnoreCase("stats")) {
                System.out.println(App.webServer().eTagHelper().getCache().getStats());
              } else {
                var subcommand = args.removeFirst();
                switch (subcommand.toLowerCase()) {
                  case "del", "remove", "unset", "uncache" -> {
                    // usage: etag_cache remove <SHA256>
                    // we need at least 1 arg at this point

                    if (args.size() < 1) {
                      logger.error("Usage: {} {} <CacheKey>", command, subcommand);
                    } else {
                      var key = args.removeFirst();
                      App.webServer().eTagHelper().getCache().remove(key);
                      logger.info("Removed {}", key);
                    }

                  }
                }
              }
            }
            case "room", "rooms" -> {
              if (args.isEmpty() || args.getFirst().equalsIgnoreCase("list")) {
                var sb = new StringBuilder();
                var rooms = App.webServer().wsListener().getLobby().createdRooms();
                var totalSockets = 0;

                sb.append("Rooms\n");
                for (var room : App.webServer().wsListener().getLobby().createdRooms()) {
                  int sockets = room.getSockets().size();
                  totalSockets += sockets;
                  sb.append("\t").append('"').append(room.name).append('"').append(" - ").append(sockets).append(" sockets\n");
                }
                sb.append("--- Total Rooms: ").append(rooms.size()).append(", Sockets: ").append(totalSockets).append(" ---");

                System.out.println(sb);
              } else {
                var subcommand = args.removeFirst();
                switch (subcommand.toLowerCase()) {
                  case "close" -> {
                    // usage: room close <NAME>...
                    // we need at least 1 arg at this point
                    if (args.isEmpty()) {
                      logger.error("Usage: {} {} <NAME>...", command, subcommand);
                    } else {
                      for (var room : args) {
                        App.webServer().wsListener().getLobby().closeRoom(room);
                        logger.info("Closed room {}", room);
                      }
                    }
                  }
                  case "sweep" -> {
                    System.err.println("not yet implemented"); // TODO
                  }
                }
              }
            }
          }
        } catch (Exception e) {
          logger.error("Failed to handle scanner iteration.", e);
        }
      }
    }, "cli").start();
  }

  public void stop() {
    try {
      scanner.close();
    } catch (Exception e) {
      logger.error("Failed to close scanner.", e);
    }
  }
}
