package com.mtinge.QueryBuilder;

import com.mtinge.QueryBuilder.ops.conflict.ConflictResolution;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.update.Update;

public class InsertBuilder extends QueryBuilder {
  private String[] columns = null;
  private String[] values = null;
  private String returning = null;
  private ConflictResolution conflictResolution = null;
  private Update.Builder update = null;

  public InsertBuilder(BuilderMode mode) {
    super(mode);
  }

  public InsertBuilder(BuilderMode mode, String table) {
    super(mode, table);
  }

  public InsertBuilder columns(String... columns) {
    if (this.columns != null) {
      throw new IllegalStateException("Cannot set token 'columns' more than once.");
    }

    this.columns = columns;
    return this;
  }

  public InsertBuilder values(String... values) {
    if (this.values != null) {
      throw new IllegalStateException("Cannot set token 'values' more than once.");
    }

    this.values = values;
    return this;
  }

  public InsertBuilder returning(String returning) {
    if (this.returning != null) {
      throw new IllegalStateException("Cannot set token 'returning' more than once.");
    }

    this.returning = returning;
    return this;
  }

  public InsertBuilder onConflict(ConflictResolution resolution) {
    if (this.returning != null) {
      throw new IllegalStateException("Cannot set token 'ConflictResolution' more than once.");
    }

    this.conflictResolution = resolution;
    return this;
  }

  public InsertBuilder set(Update update) {
    if (this.update == null) {
      this.update = Update.builder();
    }

    for (var col : update.getColumns()) {
      this.update.column(col);
    }
    for (var val : update.getValues()) {
      this.update.value(val);
    }

    return this;
  }

  public InsertBuilder set(String k, String v) {
    if (this.update == null) {
      this.update = Update.builder();
    }

    this.update.add(k, v);
    return this;
  }

  @Override
  public InsertBuilder where(Filter filter) {
    super.where(filter);
    return this;
  }

  @Override
  public InsertBuilder where(String left, Object right) {
    super.where(left, right);
    return this;
  }

  @Override
  public InsertBuilder trackBind(String key, Object value) {
    super.trackBind(key, value);
    return this;
  }

  @Override
  public String build() {
    var sb = new StringBuilder();
    sb.append(mode.getToken());
    if (mode.equals(BuilderMode.INSERT)) {
      if (columns == null || columns.length == 0) {
        throw new IllegalStateException("Attempted to construct an INSERT query with no columns.");
      }
      if (values == null || values.length == 0) {
        throw new IllegalStateException("Attempted to construct an INSERT query with no values.");
      }
      if (values.length != columns.length) {
        throw new IllegalStateException("Attemtped to construct an INSERT query with mismatched columns/values. Column count: " + columns.length + ", Values count: " + values.length + ".");
      }

      // INSERT INTO <table>...
      sb.append(" INTO ").append(table).append(" (");
      int i = 0;
      for (var col : columns) {
        i++;
        sb.append(col);

        if (i < columns.length) {
          sb.append(",");
        }
      }
      sb.append(") VALUES (");
      i = 0;
      for (var val : values) {
        i++;
        sb.append(val);

        if (i < values.length) {
          sb.append(",");
        }
      }
      sb.append(")");

      if (this.conflictResolution != null) {
        sb.append(" ON CONFLICT ").append(conflictResolution.build());
      }
    } else if (mode.equals(BuilderMode.UPDATE)) {
      if (update == null) {
        throw new IllegalStateException("Attempted to construct an UPDATE query with no updates.");
      }

      // UPDATE <table>...
      sb.append(" ").append(table).append(" SET ").append(update.build().build());

      if (filter != null) {
        sb.append(" WHERE ").append(filter.buildAssertion());
      }
    } else {
      throw new IllegalStateException("Unhandled mode token: " + mode.getToken());
    }

    if (this.returning != null) {
      sb.append(" RETURNING ").append(this.returning);
    }

    return sb.toString();
  }
}
