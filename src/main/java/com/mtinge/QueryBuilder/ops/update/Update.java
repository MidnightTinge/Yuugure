package com.mtinge.QueryBuilder.ops.update;

import java.util.LinkedList;

public class Update {
  private String[] columns = null;
  private String[] values = null;

  public Update columns(String... columns) {
    if (this.columns != null) {
      throw new IllegalStateException("Cannot set 'columns' more than once.");
    }

    this.columns = columns;
    return this;
  }

  public Update values(String... values) {
    if (this.values != null) {
      throw new IllegalStateException("Cannot set 'values' more than once.");
    }

    this.values = values;
    return this;
  }

  public String build() {
    if (columns == null || columns.length == 0) {
      throw new IllegalStateException("Cannot construct an UPDATE conflict resolution with no columns.");
    }
    if (values == null || values.length == 0) {
      throw new IllegalStateException("Cannot construct an UPDATE conflict resolution with no values.");
    }
    if (columns.length != values.length) {
      throw new IllegalStateException("Attempted to construct an UPDATE conflict resolution with mismatched columns/values. Columns: " + columns.length + ", Values: " + values.length + ".");
    }
    var sb = new StringBuilder();

    for (int i = 0; i < columns.length; i++) {
      sb.append(columns[i]).append(" = ").append(values[i]);

      if (i < columns.length - 1) {
        sb.append(", ");
      }
    }

    return sb.toString();
  }

  public String[] getColumns() {
    return columns;
  }

  public String[] getValues() {
    return values;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private LinkedList<String> columns = new LinkedList<>();
    private LinkedList<String> values = new LinkedList<>();

    public Builder add(String column, String value) {
      this.columns.add(column);
      this.values.add(value);
      return this;
    }

    public Builder column(String column) {
      this.columns.add(column);
      return this;
    }

    public Builder value(String value) {
      this.values.add(value);
      return this;
    }

    public Update build() {
      return new Update()
        .columns(columns.toArray(new String[0]))
        .values(values.toArray(new String[0]));
    }
  }
}
