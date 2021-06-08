package com.mtinge.yuugure.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public final class Validators {
  public static final class Email {
    private static final Logger logger = LoggerFactory.getLogger("EmailValidator");

    private static final Object _lock = new Object();
    private static LinkedList<String> TLDs = null;

    /**
     * Lazy-load the TLDs list from the jar resources. Will cache for future accesses.
     *
     * @return The list of TLDs from the jar's resources/TLDs.txt
     */
    public static LinkedList<String> TLDs() {
      synchronized (_lock) {
        if (TLDs != null) {
          return TLDs;
        }
        TLDs = new LinkedList<>();

        var stream = Validators.class.getClassLoader().getResourceAsStream("TLDs.txt");
        if (stream == null) {
          logger.error("TLDs stream was null");
        } else {
          try (var br = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = br.readLine()) != null) {
              TLDs.add(line.trim().toUpperCase());
            }
          } catch (Exception e) {
            logger.error("Failed to read TLDs stream.", e);
          }
        }
      }

      return TLDs;
    }

    public static boolean validEmail(String input) {
      var split = input.split("\\.");
      if (split.length == 0) {
        return false;
      }

      // pulled from https://owasp.org/www-community/OWASP_Validation_Regex_Repository
      var phase1 = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$").matcher(input).find();
      var domain = split[split.length - 1].toUpperCase();

      return phase1 && TLDs().contains(domain);
    }
  }

  public static final class Text {
    private final List<int[]> DEFAULT_CODEPOINTS = Arrays.asList(
      new int[]{0x0000, 0x007F} // basic latin
//      new int[]{0x00A1, 0x024F}, // subset of latin-1 supplement (printables, no controls)
//      new int[]{0x0400, 0x04FF}, // cyrillic
//      new int[]{0x30A0, 0x30FF}, // katakana
//      new int[]{0x3040, 0x309F}, // hiragana
//      new int[]{0x4E00, 0x9FFF}, // CJK Unified Ideographs
    );

    private String input;

    public Text(String input) {
      this.input = input;
    }

    public boolean length(int min) {
      return this.input.trim().length() >= min;
    }

    public boolean length(int min, int max) {
      var len = this.input.trim().length();
      return len >= min && len <= max;
    }

    public boolean isAlphaNumeric() {
      return this.input.trim().matches("[a-zA-Z0-9]");
    }

    public boolean matchesCodepoints() {
      return matchesCodepoints(DEFAULT_CODEPOINTS);
    }

    public boolean matchesCodepoints(List<int[]> codepoints) {
      return input.codePoints().allMatch(i -> codepoints.stream().anyMatch(pair -> (pair.length == 1) ? (i == pair[0]) : ((i >= pair[0]) && (i <= pair[1]))));
    }
  }
}
