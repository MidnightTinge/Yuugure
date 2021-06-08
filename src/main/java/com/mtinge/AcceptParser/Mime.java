package com.mtinge.AcceptParser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class Mime {
  private String type;
  private String subtype;

  /**
   * Gets the current Mime as a Mime-Type string
   *
   * @return The Mime-Type string representation.
   */
  public String asString() {
    return type + "/" + subtype;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Mime) {
      var mime = (Mime) o;
      return mime.getType().equals(type) && mime.getSubtype().equals(subtype);
    } else if (o instanceof String) {
      return asString().equals(o);
    } else return false;
  }

  /**
   * Parses a Mime-Type into a {@link Mime}
   *
   * @param str The Mime-Type to parse.
   *
   * @return The parsed {@link Mime} if the provided {@code str} was valid.
   *
   * @throws IllegalArgumentException If the provided mime-type was invalid.
   */
  public static Mime parse(String str) {
    if (!str.contains("/")) throw new IllegalArgumentException("Invalid mime: " + str);
    var split = str.split("/");
    return new Mime(split[0].toLowerCase().trim(), split[1].toLowerCase().trim());
  }
}
