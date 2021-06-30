package com.mtinge.yuugure.services.processor;

import lombok.AllArgsConstructor;

public class FileDimension {
  public static final Dimension[] DIMENSIONS = new Dimension[]{
    new Dimension("tiny", 256 * 256),
    new Dimension("small", 720 * 480),
    new Dimension("medium", 1280 * 720),
    new Dimension("large", 1920 * 1080),
    new Dimension("massive", 2000 * 2000),
  };

  public static String get(int pixels) {
    for (var dim : FileDimension.DIMENSIONS) {
      if (pixels <= dim.pixels) {
        return dim.name;
      }
    }

    return "massive";
  }

  @AllArgsConstructor
  private static final class Dimension {
    public final String name;
    public final int pixels;
  }
}
