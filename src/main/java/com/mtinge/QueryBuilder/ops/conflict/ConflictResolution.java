package com.mtinge.QueryBuilder.ops.conflict;

import com.mtinge.QueryBuilder.ops.update.Update;

public class ConflictResolution extends Update {
  private final ResolutionType type;

  public ConflictResolution(ResolutionType type) {
    this.type = type;
  }

  @Override
  public ConflictResolution columns(String... columns) {
    super.columns(columns);
    return this;
  }

  @Override
  public ConflictResolution values(String... values) {
    super.values(values);
    return this;
  }

  @Override
  public String build() {
    if (type.equals(ResolutionType.IGNORE)) {
      return "DO " + type.getToken();
    }

    return "DO " + type.getToken() + " " + super.build();
  }

  public static ConflictResolution UPDATE() {
    return new ConflictResolution(ResolutionType.UPDATE);
  }

  public static ConflictResolution IGNORE() {
    return new ConflictResolution(ResolutionType.IGNORE);
  }
}
