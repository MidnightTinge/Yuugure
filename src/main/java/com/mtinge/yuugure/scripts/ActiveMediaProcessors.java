package com.mtinge.yuugure.scripts;

import com.mtinge.yuugure.App;

import java.util.LinkedList;

public class ActiveMediaProcessors extends RunnableScript {
  public ActiveMediaProcessors() {
    super("activeMediaProcessors");
  }

  @Override
  public void run(LinkedList<String> args) {
    ActiveMediaProcessors.run();
  }

  public static void run() {
    // Grab any ProcessingQueue items that are currently in progress.
    var active = App.database().jdbi().withHandle(handle -> App.database().processors.readActive(handle));
    var sb = new StringBuilder();
    sb.append("Active Processors:\n");
    for (var item : active) {
      sb.append("\t").append("ID: ").append(item.id).append(", Upload: ").append(item.upload).append("\n");
    }
    sb.append("--- Total: ").append(active.size()).append(" ---");

    System.out.println(sb);
  }
}
