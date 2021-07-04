package com.mtinge.QueryBuilder;

import com.mtinge.QueryBuilder.ops.filter.Filter;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.Update;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * <p>QueryBuilder is meant to be a fluent SQL builder allowing the easier creation of dynamic
 * queries. It does the bare minimum input validation and assumes you have provided all of the
 * information you need and/or want provided. It does not and will not hold your hand outside of
 * basic accidental override checks.</p>
 * <p>QueryBuilder is not complete by any means, it is built specifically for Yuugure and as such
 * some basic SQL features are not implemented. That said, what is implemented should work well
 * enough for the queries that power this app, and as more functionality is needed upstream we will
 * update this downstream.</p>
 *
 * @author MidnightTinge
 */
public abstract class QueryBuilder implements IBuilder {
  protected final BuilderMode mode;
  protected final LinkedHashMap<String, Object> trackedBinds = new LinkedHashMap<>();

  protected String table;
  protected Filter filter = null;

  public QueryBuilder(BuilderMode mode) {
    this.mode = mode;
  }

  public QueryBuilder(BuilderMode mode, String table) {
    this.mode = mode;
    this.table = table;
  }

  public QueryBuilder where(String left, Object right) {
    return where(Filter.of(left, right));
  }

  public QueryBuilder where(Filter filter) {
    if (this.filter != null) {
      throw new IllegalStateException("Cannot set filter more than once.");
    }

    this.filter = filter;
    return this;
  }

  public QueryBuilder trackBind(String key, Object value) {
    trackedBinds.put(key, value);
    return this;
  }

  public Query toQuery(Handle handle) {
    return toQuery(handle, true);
  }

  public Query toQuery(Handle handle, boolean autoBind) {
    var ret = handle.createQuery(build());
    if (autoBind) {
      _bind(ret);
    }

    return ret;
  }

  public Update toUpdate(Handle handle) {
    return toUpdate(handle, true);
  }

  public Update toUpdate(Handle handle, boolean autoBind) {
    var ret = handle.createUpdate(build());
    if (autoBind) {
      _bind(ret);
    }

    return ret;
  }

  private void _bind(SqlStatement<?> handle) {
    for (var bind : trackedBinds.entrySet()) {
      handle.bind(bind.getKey(), bind.getValue());
    }
  }

  public LinkedList<Tuple<String, Object>> getTrackedBinds() {
    var ret = new LinkedList<Tuple<String, Object>>();

    for (var entry : trackedBinds.entrySet()) {
      ret.add(new Tuple<>(entry.getKey(), entry.getValue()));
    }

    return ret;
  }

  public static FetchBuilder select(String toSelect) {
    return new FetchBuilder(BuilderMode.SELECT).setSelectable(toSelect);
  }

  public static InsertBuilder update(String table) {
    return new InsertBuilder(BuilderMode.UPDATE, table);
  }

  public static FetchBuilder delete(String table) {
    return new FetchBuilder(BuilderMode.DELETE, table);
  }

  public static InsertBuilder insert(String table) {
    return new InsertBuilder(BuilderMode.INSERT, table);
  }
}
