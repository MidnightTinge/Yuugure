package com.mtinge.yuugure.services.database.props;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public final class AuditProps {
  private Integer account;
  private String targetType;
  private Integer targetId;
  private String action;
  private String details;
  private Timestamp timestamp;
}
