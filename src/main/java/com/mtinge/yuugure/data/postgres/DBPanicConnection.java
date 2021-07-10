package com.mtinge.yuugure.data.postgres;

import lombok.SneakyThrows;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.beans.ConstructorProperties;
import java.net.InetAddress;
import java.sql.Timestamp;

public class DBPanicConnection {
  @ColumnName("addr")
  public final byte[] addr;
  @ColumnName("hits")
  public final int hits;
  @ColumnName("timestamp")
  public final Timestamp timestamp;
  @ColumnName("expires")
  public final Timestamp expires;

  @ConstructorProperties({"addr", "hits", "timestamp", "expires"})
  public DBPanicConnection(byte[] addr, int hits, Timestamp timestamp, Timestamp expires) {
    this.addr = addr;
    this.hits = hits;
    this.timestamp = timestamp;
    this.expires = expires;
  }

  @SneakyThrows
  public InetAddress addrAsInet() {
    return InetAddress.getByAddress(addr);
  }
}
