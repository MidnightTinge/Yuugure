package com.mtinge.yuugure.core;

import com.mtinge.yuugure.core.adapters.DurationAdapter;
import com.mtinge.yuugure.core.adapters.InstantAdapter;
import com.mtinge.yuugure.core.adapters.ResponseAdapter;
import com.mtinge.yuugure.core.adapters.SqlTimestampAdapter;
import com.mtinge.yuugure.data.http.Response;
import com.squareup.moshi.Moshi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

public class MoshiFactory {
  private static final Logger logger = LoggerFactory.getLogger(MoshiFactory.class);

  private MoshiFactory() {
    //
  }

  public static Moshi create() {
    return new Moshi.Builder()
      .add(Duration.class, new DurationAdapter())
      .add(Instant.class, new InstantAdapter())
      .add(Response.class, new ResponseAdapter())
      .add(Timestamp.class, new SqlTimestampAdapter())
      .build();
  }

}
