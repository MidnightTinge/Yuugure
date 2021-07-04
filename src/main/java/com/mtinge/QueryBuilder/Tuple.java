package com.mtinge.QueryBuilder;

public class Tuple<T1, T2> {
  public final T1 left;
  public final T2 right;

  public Tuple(T1 left, T2 right) {
    this.left = left;
    this.right = right;
  }
}
