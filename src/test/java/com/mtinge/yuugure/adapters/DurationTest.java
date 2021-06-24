package com.mtinge.yuugure.adapters;

import com.mtinge.yuugure.core.adapters.DurationAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class DurationTest {
  private final Moshi moshi;
  private final JsonAdapter<Marshallable> ada;

  public DurationTest() {
    this.moshi = new Moshi.Builder()
      .add(Duration.class, new DurationAdapter())
      .build();
    this.ada = this.moshi.adapter(Marshallable.class);
  }

  @Test
  public void toJson() {
    assertEquals("{\"duration\":\"741ms\"}", ada.toJson(new Marshallable(Duration.ofMillis(741))));
  }

  @Test
  public void fromJson() throws IOException {
    var marshalled = ada.fromJson("{\"duration\":\"741ms\"}");
    assertNotNull(marshalled);
    assertNotNull(marshalled.duration);
    assertEquals(741L, marshalled.duration.toMillis());
  }

  @Test
  @DisplayName("Serializes round-trip correctly with random a value")
  public void randomRoundTrip() throws Exception {
    var ms = (long) (Math.random() * 10000000);

    var toJson = ada.toJson(new Marshallable(Duration.ofMillis(ms)));
    assertNotNull(toJson);
    assertFalse(toJson.isBlank());

    var fromJson = moshi.adapter(Marshallable.class).fromJson(toJson);
    assertNotNull(fromJson);
    assertNotNull(fromJson.duration);
    assertEquals(ms, fromJson.duration.toMillis());
  }

  @Test
  @DisplayName("Throws on bad duration when parsing JSON")
  public void throwsOnBadDurationFromString() {
    assertThrows(JsonDataException.class, () -> {
      ada.fromJson("{\"duration\": \"1z\"}");
    });
  }

  private static final class Marshallable {
    private Duration duration;

    public Marshallable(Duration duration) {
      this.duration = duration;
    }
  }
}
