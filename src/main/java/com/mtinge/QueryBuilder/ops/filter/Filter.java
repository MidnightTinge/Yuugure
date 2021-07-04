package com.mtinge.QueryBuilder.ops.filter;

import com.mtinge.QueryBuilder.QueryBuilder;
import com.mtinge.QueryBuilder.Tuple;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

public class Filter {
  private FilterType type;
  private Tuple<String, String> assertion = null;
  private LinkedList<Filter> nested = new LinkedList<>();
  private String op = "=";

  private Filter(String left, String right) {
    this.type = FilterType.OF;
    this.assertion = new Tuple<>(left, right);
  }

  private Filter(FilterType type, String left, String right) {
    this.type = type;
    this.assertion = new Tuple<>(left, right);
  }

  private Filter(FilterType type, Filter... filters) {
    this.type = type;
    // filter out null entries
    this.nested.addAll(Arrays.stream(filters).filter(Objects::nonNull).collect(Collectors.toList()));
  }

  public Filter append(Filter toAppend) {
    if (assertion != null) {
      nested.add(Filter.of(assertion.left, assertion.right));
      assertion = null;
    }

    nested.add(toAppend);
    return this;
  }

  public Filter setOp(String op) {
    this.op = op;
    return this;
  }

  public String buildAssertion() {
    var sb = new StringBuilder();

    if (nested != null && nested.size() > 0) {
      sb.append("(");
      if (type.equals(FilterType.NOT)) {
        sb.append(type.getToken()).append(" ");
      }

      int i = 0;
      for (var n : nested) {
        i++;
        sb.append(n.buildAssertion());

        if (i < nested.size()) {
          sb.append(" ").append(type.getToken()).append(" ");
        }
      }
      sb.append(")");
    } else if (assertion != null) {
      if (!type.equals(FilterType.OF)) {
        sb.append(type.getToken()).append(" ");
      }
      sb.append(assertion.left);
      if (assertion.right != null) {
        sb.append(op).append(assertion.right);
      }
    } else {
      throw new IllegalStateException("QueryFilter requires either an assertion or nested rules.");
    }

    return sb.toString();
  }

  public String toString() {
    if (assertion == null) {
      return "<QueryFilter type={\"" + type + "\"}>";
    } else {
      var _assertion = assertion.right != null ? (assertion.left + " " + op + " " + assertion.right) : (assertion.left);
      return "<QueryFilter type={\"" + type + "\"} assertion={\"" + _assertion + "\"}>";
    }
  }

  public FilterType getType() {
    return type;
  }

  public LinkedList<Filter> getNested() {
    return nested;
  }

  public Tuple<String, String> getAssertion() {
    return assertion;
  }

  public static Filter of(String left, Object right) {
    String sql;
    if (right instanceof QueryBuilder) {
      sql = "(" + ((QueryBuilder) right).build() + ")";
    } else {
      sql = String.valueOf(right);
    }

    return new Filter(left, sql);
  }

  public static Filter of(String assertion) {
    return new Filter(assertion, null);
  }


  public static Filter in(String left, String... right) {
    return new Filter(left + " IN (" + String.join(",", right) + ")", null);
  }


  public static Filter or(Filter... filters) {
    return new Filter(FilterType.OR, filters);
  }


  public static Filter and(Filter... filters) {
    return new Filter(FilterType.AND, filters);
  }


  public static Filter not(String assertion) {
    return new Filter(FilterType.NOT, assertion, null);
  }

  public static Filter not(String left, Object right) {
    return new Filter(FilterType.NOT, left, String.valueOf(right));
  }

  public static Filter not(Filter... filters) {
    return new Filter(FilterType.NOT, filters);
  }
}
