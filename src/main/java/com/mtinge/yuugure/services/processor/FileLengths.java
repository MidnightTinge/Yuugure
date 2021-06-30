package com.mtinge.yuugure.services.processor;

import lombok.AllArgsConstructor;

import java.time.Duration;

public class FileLengths {
  public static final Length[] LENGTHS = new Length[]{
    new Length("very_short", Duration.ofSeconds(10).toSeconds()),
    new Length("short", Duration.ofSeconds(30).toSeconds()),
    new Length("medium", Duration.ofMinutes(5).toSeconds()),
    new Length("long", Duration.ofMinutes(15).toSeconds()),
  };

  public static String get(double lengthMillis) {
    for (var length : FileLengths.LENGTHS) {
      if (lengthMillis <= length.max) {
        return length.name;
      }
    }

    return "verylong";
  }

  @AllArgsConstructor
  private static final class Length {
    public final String name;
    public final long max;
  }
}
