package com.mtinge.yuugure.services.database;

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
}
