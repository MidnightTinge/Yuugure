package com.mtinge.yuugure.services.processor;

import lombok.AllArgsConstructor;

public class FileSize {
  public static final int MB = 1024 * 1024;

  public static final Size[] SIZES = new Size[]{
    new Size("tiny", null, MB),          // [0, 1mb)
    new Size("small", MB, MB * 5),       // [1mb, 5mb)
    new Size("medium", MB * 5, MB * 15), // [5mb, 15mb)
    new Size("large", MB * 15, MB * 30), // [15mb, 30mb)
    new Size("massive", MB * 30, null)   // (30mb,)
  };

  public static String get(int bytelen) {
    for (var size : FileSize.SIZES) {
      var minMatch = size.min == null || bytelen <= size.min;
      var maxMatch = size.max == null || bytelen <= size.max;
      if (minMatch && maxMatch) {
        return size.name;
      }
    }

    return "massive";
  }

  @AllArgsConstructor
  private static final class Size {
    public final String name;
    public final Integer min;
    public final Integer max;
  }
}
