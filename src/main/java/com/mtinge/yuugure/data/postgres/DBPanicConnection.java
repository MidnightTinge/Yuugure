package com.mtinge.yuugure.data.postgres;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jdbi.v3.core.mapper.RowMapper;

import java.net.InetAddress;
import java.sql.Timestamp;

@AllArgsConstructor
public class DBPanicConnection {
  public final byte[] addr;
  public final int hits;
  public final Timestamp timestamp;
  public final Timestamp expires;

  @SneakyThrows
  public InetAddress getInet() {
    return InetAddress.getByAddress(addr);
  }

  public static final RowMapper<DBPanicConnection> Mapper = (r, c) -> new DBPanicConnection(
    r.getBytes("addr"),
    r.getInt("hits"),
    r.getTimestamp("timestamp"),
    r.getTimestamp("expires")
  );

}
