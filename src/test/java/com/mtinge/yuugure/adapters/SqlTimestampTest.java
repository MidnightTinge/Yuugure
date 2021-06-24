package com.mtinge.yuugure.adapters;

import com.mtinge.yuugure.core.adapters.SqlTimestampAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SqlTimestampTest {
  private final Moshi moshi;
  private final JsonAdapter<Marshallable> ada;

  public SqlTimestampTest() {
    this.moshi = new Moshi.Builder()
      .add(Timestamp.class, new SqlTimestampAdapter())
      .build();
    this.ada = this.moshi.adapter(Marshallable.class);
  }

  @Test
  public void toJson() {
    Instant now = Instant.now();
    Timestamp timestamp = Timestamp.from(now);

    String expected = "{\"timestamp\":" + now.toEpochMilli() + "}";
    assertEquals(expected, ada.toJson(new Marshallable(timestamp)));
  }

  @Test
  public void fromJson() throws IOException {
    Instant now = Instant.now();
    Timestamp timestamp = Timestamp.from(now);

    String json = "{\"timestamp\":" + now.toEpochMilli() + "}";
    Marshallable marshalled = ada.fromJson(json);

    assertNotNull(marshalled);
    assertNotNull(marshalled.timestamp);
    assertEquals(now.toEpochMilli(), marshalled.timestamp.toInstant().toEpochMilli());
  }

  private static final class Marshallable {
    private Timestamp timestamp;

    public Marshallable(Timestamp timestamp) {
      this.timestamp = timestamp;
    }
  }
}
