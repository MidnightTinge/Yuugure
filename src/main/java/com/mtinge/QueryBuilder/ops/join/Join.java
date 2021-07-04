package com.mtinge.QueryBuilder.ops.join;

import com.mtinge.QueryBuilder.Tuple;

public class Join {
  private final JoinType type;
  private final Tuple<String, String> join;

  private Join(JoinType type, Tuple<String, String> join) {
    this.type = type;
    this.join = join;
  }

  public static Join fullOuter(String table, String condition) {
    return new Join(JoinType.FULL_OUTER, new Tuple<>(table, condition));
  }

  public static Join leftOuter(String table, String condition) {
    return new Join(JoinType.LEFT_OUTER, new Tuple<>(table, condition));
  }

  public static Join rightOuter(String table, String condition) {
    return new Join(JoinType.RIGHT_OUTER, new Tuple<>(table, condition));
  }

  public static Join inner(String table, String condition) {
    return new Join(JoinType.INNER, new Tuple<>(table, condition));
  }

  public Tuple<String, String> getJoin() {
    return join;
  }

  public JoinType getType() {
    return type;
  }
}
