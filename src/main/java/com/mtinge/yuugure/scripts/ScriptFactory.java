package com.mtinge.yuugure.scripts;

import java.util.HashMap;

public class ScriptFactory {
  private HashMap<String, RunnableScript> scripts = new HashMap<>();

  public ScriptFactory register(RunnableScript... scripts) {
    for (var script : scripts) {
      this.scripts.put(script.name.toLowerCase().trim(), script);
    }

    return this;
  }

  public RunnableScript get(String name) {
    return scripts.get(name.toLowerCase().trim());
  }

  public Iterable<RunnableScript> getRegistered() {
    return scripts.values();
  }
}
