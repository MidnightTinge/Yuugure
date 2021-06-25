package com.mtinge.yuugure.scripts;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.data.postgres.DBProcessingQueue;

import java.util.LinkedList;
import java.util.stream.Collectors;

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
    var active = App.database().jdbi().withHandle(handle ->
      handle.createQuery("SELECT * FROM processing_queue WHERE dequeued AND NOT (finished OR errored)")
        .map(DBProcessingQueue.Mapper)
        .collect(Collectors.toList())
    );
    var sb = new StringBuilder();
    sb.append("Active Processors:\n");
    for (var item : active) {
      sb.append("\t").append("ID: ").append(item.id).append(", Upload: ").append(item.upload).append("\n");
    }
    sb.append("--- Total: ").append(active.size()).append(" ---");

    System.out.println(sb);
  }
}
