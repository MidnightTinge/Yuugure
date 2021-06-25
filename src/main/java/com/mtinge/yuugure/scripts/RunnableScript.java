package com.mtinge.yuugure.scripts;

import lombok.AllArgsConstructor;

import java.util.LinkedList;

@AllArgsConstructor
public abstract class RunnableScript {
  public final String name;

  public abstract void run(LinkedList<String> args);
}
