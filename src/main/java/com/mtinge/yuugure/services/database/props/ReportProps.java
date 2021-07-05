package com.mtinge.yuugure.services.database.props;

import com.mtinge.yuugure.data.postgres.ReportTargetType;
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
public class ReportProps {
  /**
   * Whether or not this report is active (open/closed).
   */
  private Boolean active = null;
  /**
   * The reporter's account ID.
   */
  private Integer account = null;
  /**
   * When this report was received.
   */
  private Timestamp timestamp = null;
  /**
   * If this report has been claimed by a staff member.
   */
  private Boolean claimed = null;
  /**
   * The staff member's ID that claimed this report.
   */
  private Integer claimedBy = null;
  /**
   * The dynamic target type, e.g. "upload"|"user".
   */
  private ReportTargetType targetType = null;
  /**
   * The dynamic target ID. If the targetType is a user, then this would correspond to the reported
   * user's ID.
   */
  private Integer targetId = null;
  /**
   * The user's reason for reporting, e.g. "breaks rule #3"
   */
  private String content = null;
}
