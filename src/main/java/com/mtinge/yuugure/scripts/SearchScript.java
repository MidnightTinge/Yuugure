package com.mtinge.yuugure.scripts;

import com.mtinge.yuugure.App;
import com.mtinge.yuugure.core.MoshiFactory;

import java.util.LinkedList;

public class SearchScript extends RunnableScript {
  public SearchScript() {
    super("Search");
  }

  @Override
  public void run(LinkedList<String> args) {
    if (!args.isEmpty()) {
      SearchScript.run(String.join(" ", args));
    } else {
      System.err.println("Missing search query");
    }
  }

  public static void run(String query) {
    try {
      System.out.println(
        MoshiFactory.create().adapter(Object.class).indent("  ").toJson(
          App.elastic().search(query, 1)
        )
      );
    } catch (Throwable t) {
      System.err.println("Failed to run search");
      t.printStackTrace();
    }
  }
}
