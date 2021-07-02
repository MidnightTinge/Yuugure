package com.mtinge.yuugure.services.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public class BookmarkFetchParams {
  public boolean includePrivate = false;
  public boolean includeInactive = false;
}
