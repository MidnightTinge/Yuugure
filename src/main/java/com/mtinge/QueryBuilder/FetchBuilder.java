package com.mtinge.QueryBuilder;

import com.mtinge.QueryBuilder.ops.For.ForType;
import com.mtinge.QueryBuilder.ops.filter.Filter;
import com.mtinge.QueryBuilder.ops.join.Join;
import com.mtinge.QueryBuilder.ops.order.OrderType;
import com.mtinge.QueryBuilder.ops.union.Union;

import java.util.LinkedList;
import java.util.Objects;

/**
 * A {@link QueryBuilder} used for fetch-style queries (SELECT, UPDATE)
 *
 * @see QueryBuilder
 */
public class FetchBuilder extends QueryBuilder {
  private String selectable = null;

  private Integer limit = null;
  private LinkedList<Join> joins = new LinkedList<>();
  private LinkedList<Tuple<String, OrderType>> sorts = new LinkedList<>();
  private LinkedList<String> grouping = new LinkedList<>();
  private Filter having = null;
  private Union union = null;
  private ForType _for = null;

  public FetchBuilder(BuilderMode mode) {
    super(mode);
  }

  public FetchBuilder(BuilderMode mode, String table) {
    super(mode, table);
  }

  /**
   * Set the "FROM" token's value. Can only be called once.
   *
   * @param table The "FROM" token's value.
   *
   * @return This {@link FetchBuilder} object for chaining.
   *
   * @throws IllegalStateException if the "FROM" token has already been given a value.
   */
  public FetchBuilder from(String table) {
    if (this.table != null) {
      throw new IllegalStateException("Cannot set table multiple times");
    }

    this.table = table;
    return this;
  }

  /**
   * Set a nested query as the "FROM" token.
   *
   * @param nested The nested query.
   * @param name The name of the query result.
   *
   * @return This {@link FetchBuilder} for chaining.
   */
  public FetchBuilder from(QueryBuilder nested, String name) {
    if (this.table != null) {
      throw new IllegalStateException("Cannot set table multiple times");
    }

    this.table = "(" + nested.build() + ") " + name;
    return this;
  }

  public FetchBuilder join(Join join) {
    joins.add(join);
    return this;
  }

  public FetchBuilder order(String condition) {
    order(condition, OrderType.ASC);
    return this;
  }

  public FetchBuilder order(String condition, OrderType orderType) {
    sorts.add(new Tuple<>(condition, orderType));
    return this;
  }

  public FetchBuilder limit(int limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public FetchBuilder where(Filter filter) {
    super.where(filter);
    return this;
  }

  @Override
  public FetchBuilder where(String left, Object right) {
    super.where(left, right);
    return this;
  }

  public FetchBuilder group(String grouping) {
    this.grouping.add(grouping);
    return this;
  }

  public FetchBuilder having(Filter condition) {
    if (this.having != null) {
      throw new IllegalStateException("Cannot set 'having' token more than once.");
    }

    this.having = condition;
    return this;
  }

  public FetchBuilder union(Union union) {
    if (this.union != null) {
      throw new IllegalStateException("Cannot set 'union' token more than once.");
    }

    this.union = union;
    return this;
  }

  public FetchBuilder withFor(ForType forType) {
    if (this._for != null) {
      throw new IllegalStateException("Cannot set 'for' token more than once.");
    }

    this._for = forType;
    return this;
  }

  @Override
  public FetchBuilder bind(String key, Object value) {
    super.bind(key, value);
    return this;
  }

  @Override
  public String build() {
    Objects.requireNonNull(mode);
    Objects.requireNonNull(table);
    if (mode.equals(BuilderMode.SELECT) && selectable == null) {
      throw new IllegalStateException("Cannot construct a 'SELECT' builder without selected columns.");
    }

    var sb = new StringBuilder();

    sb.append(mode).append(" ");
    if (selectable != null) {
      // common for delete/update operations.
      sb.append(selectable).append(" ");
    }

    sb.append("FROM ").append(table).append(" ");

    if (!joins.isEmpty()) {
      for (var join : joins) {
        var op = join.getJoin();
        sb.append(join.getType().getToken()).append(" JOIN ").append(op.left).append(" ON ").append(op.right).append(" ");
      }
    }

    if (filter != null) {
      sb.append("WHERE ").append(filter.buildAssertion()).append(" ");
    }

    // group by
    if (!grouping.isEmpty()) {
      sb.append("GROUP BY ");
      int i = 0;
      for (var group : grouping) {
        sb.append(group).append(" ");
      }
    }
    // having
    if (this.having != null) {
      sb.append("HAVING ").append(having.buildAssertion()).append(" ");
    }
    // union
    if (this.union != null) {
      sb.append("UNION ").append(union.build()).append(" ");
    }

    if (!sorts.isEmpty()) {
      sb.append("ORDER BY ");
      int i = 0;
      for (var sort : sorts) {
        i++;
        sb.append(sort.left).append(" ").append(sort.right.getToken());

        if (i < sorts.size()) {
          sb.append(", ");
        }
      }
    }

    if (limit != null) {
      sb.append(" LIMIT ").append(limit);
    }

    if (_for != null) {
      sb.append(" FOR ").append(_for.getToken());
    }

    return sb.toString();
  }

  FetchBuilder setSelectable(String selectable) {
    this.selectable = selectable;
    return this;
  }
}
