package com.mtinge.yuugure.scripts;

import com.mtinge.TagTokenizer.TagTokenizer;
import com.mtinge.yuugure.App;

import java.util.LinkedList;

public class DslScript extends RunnableScript {
  public DslScript() {
    super("DSL");
  }

  @Override
  public void run(LinkedList<String> args) {
    if (!args.isEmpty()) {
      DslScript.run(String.join(" ", args));
    } else {
      System.err.println("Missing search query");
    }
  }

  public static void run(String query) {
    try {
      System.out.println(App.tagManager().buildQuery(TagTokenizer.parse(query)));
    } catch (Throwable t) {
      System.err.println("Failed to get DSL");
      t.printStackTrace();
    }
  }

}
