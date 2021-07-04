package com.mtinge.QueryBuilder.ops.union;

import com.mtinge.QueryBuilder.QueryBuilder;

public class Union {
  private final UnionSpecifier specifier;
  private final UnionType type;
  private final QueryBuilder expression;

  public Union(UnionType type, UnionSpecifier specifier, QueryBuilder expression) {
    this.type = type;
    this.specifier = specifier;
    this.expression = expression;
  }

  public UnionSpecifier getSpecifier() {
    return specifier;
  }

  public UnionType getType() {
    return type;
  }

  public QueryBuilder getExpression() {
    return expression;
  }

  public String build() {
    return this.type.getToken() + " " + this.specifier.getToken() + " (" + expression.build() + ")";
  }

  public static Union union(UnionSpecifier specifier, QueryBuilder expression) {
    return new Union(UnionType.UNION, specifier, expression);
  }

  public static Union intersect(UnionSpecifier specifier, QueryBuilder expression) {
    return new Union(UnionType.INTERSECT, specifier, expression);
  }

  public static Union except(UnionSpecifier specifier, QueryBuilder expression) {
    return new Union(UnionType.EXCEPT, specifier, expression);
  }
}
