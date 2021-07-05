package com.mtinge.yuugure.services.database.providers;

import org.jdbi.v3.core.Handle;

public abstract class Provider<X, Y> implements IProvider<X, Y> {
  public static final int FAIL_UNKNOWN = -99;
  public static final int FAIL_SQL = -98;

  protected void requireTransaction(Handle handle) {
    if (!handle.isInTransaction()) {
      throw new IllegalStateException("This action's handle must be in a transaction.");
    }
  }
}
