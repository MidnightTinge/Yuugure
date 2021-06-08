package com.mtinge.AcceptParser;

import lombok.Getter;

import java.util.LinkedList;

public class ParserResult {
  private LinkedList<Entry> entries;
  @Getter
  private Entry heaviest;

  ParserResult(LinkedList<Entry> entries, Entry heaviest) {
    this.entries = entries;
    this.heaviest = heaviest;
  }

  /**
   * Gets a copy of this parser's entries.
   *
   * @return A copy of this parser's {@link Entry} list as an array.
   */
  public Entry[] getEntries() {
    return entries.toArray(new Entry[0]);
  }

  /**
   * Finds the best match for a given set of {@link Mime}s based on the {@link Mime}'s weight.
   *
   * @param search The mime to search for.
   *
   * @return The best matching {@link Mime} if found, or {@code null} otherwise.
   */
  public Entry bestMatch(Mime... search) {
    Entry bestMatch = null;

    for (Mime mime : search) {
      for (var entry : entries) {
        if (entry.getMime().getType().equals(mime.getType()) || entry.getMime().getType().equals("*")) {
          if (entry.getMime().getSubtype().equals(mime.getSubtype()) || entry.getMime().getSubtype().equals("*")) {
            if (bestMatch == null) {
              bestMatch = entry;
            } else if (entry.getWeight() > bestMatch.getWeight()) {
              bestMatch = entry;
            }
          }
        }
      }
    }

    return bestMatch;
  }

  /**
   * Checks if the provided {@link Mime} is the best match for this parser.
   *
   * @param search The {@link Mime} to check.
   *
   * @return Whether or not the provided {@link Mime} is the best match for this parser.
   *
   * @see #bestMatch(Mime...)
   */
  public boolean isBestMatch(Mime search) {
    var bestMatch = bestMatch(search);
    return bestMatch != null && bestMatch.getMime().equals(search);
  }

  /**
   * Checks if the provided {@link Mime} matches at all for this parser.
   *
   * @param search The {@link Mime} to check.
   *
   * @return Whether or not the provided {@link Mime} matches for this parser.
   */
  public boolean hasMatch(Mime search) {
    for (var entry : entries) {
      if (entry.getMime().getType().equals(search.getType())) {
        if (entry.getMime().getSubtype().equals(search.getSubtype()) || entry.getMime().getSubtype().equals("*")) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Checks the quality of the provided {@link Mime}.
   *
   * @param search The {@link Mime} to search.
   *
   * @return The weight of the provided {@link Mime} if it exists in this parser. {@code null}
   *   otherwise.
   */
  public Double getQuality(Mime search) {
    var match = bestMatch(search);
    if (match != null) {
      return match.getWeight();
    }

    return null;
  }
}
