package com.mtinge.yuugure.services.database;

import com.mtinge.yuugure.core.States;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class UploadFetchParams {
  private boolean includeBadFlagged;
  private boolean includePrivate;

  public boolean hasFilter() {
    return !includeBadFlagged || !includePrivate;
  }

  public long getFilter() {
    if (!hasFilter()) {
      return 0L;
    }

    long filterState = 0L;
    filterState = includePrivate ? filterState : States.compute(filterState, States.Upload.PRIVATE);
    filterState = includeBadFlagged ? filterState : States.compute(filterState, States.Upload.DELETED, States.Upload.DMCA);

    return filterState;
  }
}
