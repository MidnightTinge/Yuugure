package com.mtinge.yuugure.services.processor;

import lombok.AllArgsConstructor;

public class FileSize {
  public static final int MB = 1024 * 1024;

  public static final Size IMAGE_TINY = new Size("tiny", 0, MB);               // [0, 1mb)
  public static final Size IMAGE_SMALL = new Size("small", MB, MB * 5);        // [1mb, 5mb)
  public static final Size IMAGE_MEDIUM = new Size("medium", MB * 5, MB * 15); // [5mb, 15mb)
  public static final Size IMAGE_LARGE = new Size("large", MB * 15, MB * 30);  // [15mb, 30mb)
  public static final Size IMAGE_MASSIVE = new Size("massive", MB * 30, null); // [30mb,)

  public static final Size VIDEO_TINY = new Size("tiny", 0, MB * 5);            // [0, 5mb)
  public static final Size VIDEO_SMALL = new Size("small", MB * 5, MB * 15);    // [5mb, 15mb)
  public static final Size VIDEO_MEDIUM = new Size("medium", MB * 15, MB * 30); // [15mb, 30mb)
  public static final Size VIDEO_LARGE = new Size("large", MB * 30, MB * 50);   // [30mb, 50mb)
  public static final Size VIDEO_MASSIVE = new Size("massive", MB * 50, null);  // [50mb,)

  public static final Size[] IMAGE_SIZES = new Size[]{
    IMAGE_TINY,
    IMAGE_SMALL,
    IMAGE_MEDIUM,
    IMAGE_LARGE,
    IMAGE_MASSIVE
  };

  public static final Size[] VIDEO_SIZES = new Size[]{
    VIDEO_TINY,
    VIDEO_SMALL,
    VIDEO_MEDIUM,
    VIDEO_LARGE,
    VIDEO_MASSIVE
  };

  public static String get(int bytelen, boolean video) {
    for (var size : (video ? VIDEO_SIZES : IMAGE_SIZES)) {
      var minMatch = size.min == null || bytelen >= size.min;
      var maxMatch = size.max == null || bytelen < size.max;

      if (minMatch && maxMatch) {
        return size.name;
      }
    }

    return "massive";
  }

  @AllArgsConstructor
  public static final class Size {
    public final String name;
    /**
     * The inclusive minimum size.
     */
    public final Integer min;
    /**
     * The exclusive maximum size.
     */
    public final Integer max;

    public String toString() {
      return "FileSize(" + name + ", min=" + min + ", max=" + max + ")";
    }
  }
}
