package com.mtinge.yuugure.core.adapters;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class DurationAdapter extends JsonAdapter<Duration> {
  private static Map<String, Duration> map = new LinkedHashMap<>();

  static {
    map.put("y", ChronoUnit.YEARS.getDuration());
    map.put("mo", ChronoUnit.MONTHS.getDuration());
    map.put("w", ChronoUnit.WEEKS.getDuration());
    map.put("d", ChronoUnit.DAYS.getDuration());
    map.put("h", ChronoUnit.HOURS.getDuration());
    map.put("m", ChronoUnit.MINUTES.getDuration());
    map.put("s", ChronoUnit.SECONDS.getDuration());
    map.put("ms", ChronoUnit.MILLIS.getDuration());
  }

  private static final Pattern PATTERN = Pattern.compile("([0-9]+)([a-z]{0,2})?", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_UNIT = Pattern.compile("mo|ms|[ywdhms]", Pattern.CASE_INSENSITIVE);

  public DurationAdapter() {
    //
  }

  @Override
  public Duration fromJson(JsonReader reader) throws IOException {
    if (reader.peek() != JsonReader.Token.STRING) throw new JsonDataException("Expected a string for duration");
    var toParse = reader.nextString();
    if (toParse.isBlank()) throw new JsonDataException("Invalid input received");

    return durationFromString(toParse);
  }

  @Override
  public void toJson(JsonWriter writer, Duration value) throws IOException {
    if (value == null) {
      writer.nullValue();
    } else {
      writer.value(durationToString(value));
    }
  }

  public static String durationToString(Duration value) {
    var sb = new StringBuilder();
    value = Duration.ofMillis(value.toMillis()); // obtain a copy
    for (var entry : map.entrySet()) {
      if (value.compareTo(entry.getValue()) < 0) continue; // our value is less than the entry

      long diff = value.dividedBy(entry.getValue());
      sb.append(diff).append(entry.getKey());
      value = value.minus(Math.multiplyExact(diff, entry.getValue().toMillis()), ChronoUnit.MILLIS);
    }

    return sb.toString();
  }

  public static Duration durationFromString(String value) {
    long total = 0L;
    var matcher = PATTERN.matcher(value);

    while (matcher.find()) {
      if (matcher.group(1) == null) throw new JsonDataException("Invalid duration provided");
      var unit = Optional.ofNullable(matcher.group(2)).map(String::toLowerCase).orElse("s");
      var duration = map.get(unit);
      if (duration == null) throw new JsonDataException("Unknown unit specifier: " + unit);

      total += ((Integer.parseInt(matcher.group(1))) * duration.toMillis());
    }

    return Duration.ofMillis(total);
  }
}
