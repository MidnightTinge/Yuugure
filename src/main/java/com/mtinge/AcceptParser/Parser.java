package com.mtinge.AcceptParser;

import java.util.LinkedList;
import java.util.Objects;

public class Parser {
  /**
   * Parses the provided {@code headerValue} into a {@link ParserResult}.
   *
   * @param headerValue The header value to parse
   *
   * @return The {@link ParserResult} for this header value.
   */
  public static ParserResult parse(String headerValue) {
    var ll = new LinkedList<Entry>();
    var mimes = headerValue.split(",");

    Entry heaviest = null;

    for (String s : mimes) {
      if (!s.contains("/")) continue;
      String mime = null;
      Double quality = null;

      for (var segment : s.split(";")) {
        if (mime == null && segment.contains("/")) {
          mime = segment;
          if (quality != null) break;
        } else if (quality == null && segment.contains("q=")) {
          Double parsedQuality = null;
          try {
            parsedQuality = Double.parseDouble(segment.substring(segment.indexOf("q=") + 2));
          } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
          }

          if (parsedQuality != null) {
            quality = parsedQuality;
            if (mime != null) break;
          }
        }
      }

      if (mime == null) continue;

      var entry = new Entry(Mime.parse(mime), Objects.requireNonNullElse(quality, 0d));
      if (heaviest == null) {
        heaviest = entry;
      } else if (entry.getWeight() > heaviest.getWeight()) {
        heaviest = entry;
      }

      ll.add(entry);
    }

    return new ParserResult(ll, heaviest);
  }
}
