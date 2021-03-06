package com.mtinge.yuugure.services.cli;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.TagManager.TagDescriptor;
import com.mtinge.yuugure.core.adapters.DurationAdapter;
import com.mtinge.yuugure.data.postgres.DBTag;
import com.mtinge.yuugure.scripts.*;
import com.mtinge.yuugure.services.IService;
import com.mtinge.yuugure.services.database.UploadFetchParams;
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
      new ActiveMediaProcessors(),
      new AddTagsFromFilesystem(),
      new SearchScript(),
      new DslScript()
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
            case "tag", "tags", "tagmanager", "tag_manager", "tm" -> {
              if (args.isEmpty() || args.getFirst().equalsIgnoreCase("list")) {
                var tags = App.tagManager().getTags();
                var sb = new StringBuilder();

                sb.append("Tags:\n");
                for (var tag : tags) {
                  sb.append("\t").append("(ID: ").append(tag.id).append(") ").append(tag.category).append(":").append(tag.name).append("\n");
                }
                sb.append("--- Total: ").append(tags.size()).append(" ---");

                System.out.println(sb);
              } else {
                var subcommand = args.removeFirst();
                switch (subcommand.toLowerCase()) {
                  case "reload" -> {
                    App.tagManager().reload();
                  }
                  case "create", "new", "put", "add" -> {
                    // tags create <category:name...>
                    // we need at least 1 args at this point.
                    if (args.size() < 1) {
                      System.out.println("Usage: " + command + " " + subcommand + " <category:name...>");
                    } else {
                      for (var tagName : args) {
                        var descriptor = TagDescriptor.parse(tagName);
                        if (descriptor != null) {
                          try {
                            var res = App.tagManager().createTag(descriptor);
                            if (!res.tags.isEmpty()) {
                              var tag = res.tags.get(0);
                              System.out.println("Created tag " + tag.id + " (" + tag.name + ")");
                            } else {
                              System.err.println("Failed to create tags. From tagManager:");
                              res.messages.forEach(System.err::println);
                            }
                          } catch (IllegalArgumentException iae) {
                            System.out.println("Failed to create tag: " + iae.getMessage());
                          }
                        } else {
                          System.out.println("Invalid tag format \"" + tagName + "\", expected \"category:name\".");
                        }
                      }
                    }
                  }
                  case "delete", "del", "remove" -> {
                    // tags delete <category:name...>
                    // we need at least 1 arg at this point.
                    if (args.size() < 1) {
                      System.out.println("Usage: " + command + " " + subcommand + " <category:name...>");
                    } else {
                      for (var tagName : args) {
                        var parsed = TagDescriptor.parse(tagName);
                        if (parsed != null) {
                          System.out.println("Deleting " + tagName + "...");
                          if (!App.tagManager().deleteTag(parsed)) {
                            System.out.println("Failed to delete tag " + tagName);
                          }
                        } else {
                          System.out.println("Bad tag input: " + tagName + ", failed to parse as a TagDescriptor (format: \"category:name\")");
                        }
                      }
                      System.out.println("All tag deletions complete.");
                    }
                  }
                  case "associate" -> {
                    // tags associate <parent> <child>
                    // we need at least 2 args at this point.
                    if (args.size() < 2) {
                      System.out.println("Usage: " + command + " " + subcommand + " <category:parent_name> <category:child_name>");
                    } else {
                      var parent = args.removeFirst();
                      var child = args.removeFirst();

                      var parentDescriptor = TagDescriptor.parse(parent);
                      var childDescriptor = TagDescriptor.parse(child);

                      if (parentDescriptor == null) {
                        System.out.println("The privded parent tag \"" + parent + "\" is in an invalid format. Expected \"category:name\"");
                      } else if (childDescriptor == null) {
                        System.out.println("The privded child tag \"" + child + "\" is in an invalid format. Expected \"category:name\"");
                      } else {
                        if (App.tagManager().setParent(childDescriptor, parentDescriptor)) {
                          System.out.println("Updated " + child + "'s parent to " + parent + ".");
                        } else {
                          System.out.println("Failed to update associations, TagManager returned false.");
                        }
                      }
                    }
                  }
                  case "disassociate" -> {
                    // tags disassociate <category:name>
                    // we need at least 2 args at this point.
                    if (args.size() < 2) {
                      System.out.println("Usage: " + command + " " + subcommand + " <category:child_name>");
                    } else {
                      var child = args.removeFirst();

                      var childDescriptor = TagDescriptor.parse(child);

                      if (childDescriptor == null) {
                        System.out.println("The privded child tag \"" + child + "\" is in an invalid format. Expected \"category:name\"");
                      } else {
                        if (App.tagManager().removeParent(childDescriptor)) {
                          System.out.println("Removed " + child + "'s parent.");
                        } else {
                          System.out.println("Failed to update associations, TagManager returned false.");
                        }
                      }
                    }
                  }
                  case "search" -> {
                    // tags search <wildcard>
                    // we need at least 1 arg at this point.
                    if (args.size() < 1) {
                      System.out.println("Usage: " + command + " " + subcommand + " <wildcard>");
                    } else {
                      var sb = new StringBuilder();
                      var search = args.removeFirst();
                      try {
                        var tags = App.tagManager().search(search);

                        sb.append("Results:\n");
                        for (var tag : tags) {
                          sb.append("\t[").append(tag.id).append("] ").append(tag.category).append(":").append(tag.name).append("\n");
                        }
                        sb.append("--- Total: ").append(tags.size()).append(" ---");

                        System.out.println(sb);
                      } catch (IllegalArgumentException iae) {
                        System.out.println("Invalid wildcard: " + iae.getMessage());
                      }
                    }
                  }
                }
              }
            }
            case "upload" -> {
              String usage = "Usage: upload <id> tag <tag:name...>\n       upload <id> untag <tag:name...>";
              if (args.isEmpty()) {
                System.out.println(usage);
              } else {
                var uid = args.removeFirst();
                if (args.isEmpty() || !uid.matches("^[0-9]+$")) {
                  System.out.println(usage);
                } else {
                  var upload = App.database().jdbi().withHandle(handle -> App.database().uploads.read(Integer.parseInt(uid), new UploadFetchParams(true, true), handle));

                  if (upload == null) {
                    System.out.println("Upload doesn't exist.");
                  } else {
                    var subcommand = args.removeFirst();
                    switch (subcommand.toLowerCase()) {
                      case "tag" -> {
                        // upload <id> tag <tag:name...>
                        // we need at least 1 arg at this point.
                        if (args.size() < 1) {
                          System.out.println("Usage: upload <id> tag <id:name...>");
                        } else {
                          var tags = new LinkedList<DBTag>();
                          for (var tagName : args) {
                            var descriptor = TagDescriptor.parse(tagName);
                            if (descriptor == null) {
                              System.out.println("Skipping tag " + tagName + " due to invalid format. Expected \"name:type\".");
                            } else {
                              var ensured = App.tagManager().ensureTag(descriptor);
                              tags.addAll(ensured.tags);
                              ensured.messages.forEach(System.err::println);
                            }
                          }

                          if (tags.isEmpty()) {
                            System.out.println("Skipping upload " + uid + " due to empty tag list.");
                          } else {
                            var updated = App.database().jdbi().inTransaction(handle -> App.database().tags.addTagsToUpload(upload.id, tags, handle));
                            if (updated) {
                              System.out.println("Tags updated.");
                            } else {
                              System.out.println("Failed to add tags, Database returned false.");
                            }
                          }
                        }
                      }
                      case "untag" -> {
                        // upload <id> untag <tag:name...>
                        // we need at least 1 arg at this point.
                        if (args.size() < 1) {
                          System.out.println("Usage: upload <id> tag <id:name...>");
                        } else {
                          var tags = new LinkedList<DBTag>();
                          for (var tagName : args) {
                            var descriptor = TagDescriptor.parse(tagName);
                            if (descriptor == null) {
                              System.out.println("Skipping tag " + tagName + " due to invalid format. Expected \"name:type\".");
                            } else {
                              var ensured = App.tagManager().ensureTag(descriptor);
                              tags.addAll(ensured.tags);
                              ensured.messages.forEach(System.err::println);
                            }
                          }

                          if (tags.isEmpty()) {
                            System.out.println("Skipping upload " + uid + " due to empty tag list.");
                          } else {
                            var updated = App.database().jdbi().inTransaction(handle -> App.database().tags.removeTagsFromUpload(upload.id, tags, handle));
                            if (updated) {
                              System.out.println("Tags updated.");
                            } else {
                              System.out.println("Failed to add tags, Database returned false.");
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            case "search" -> {
              var usage = "Usage: search (upload|account) <terms...>";
              // search (upload|account) <terms...>
              if (args.isEmpty()) {
                System.out.println(usage);
              } else {
                var domain = args.removeFirst();
                switch (domain.toLowerCase()) {
                  case "upload" -> {
                    // search upload
                  }
                  case "account" -> {
                    // search account
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
