package com.mtinge.yuugure.data.http;

import com.mtinge.yuugure.data.postgres.DBReport;
import com.squareup.moshi.Json;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class ReportResponse {
  @Json(name = "report_id")
  public final int reportId;
  public final String reason;

  public static ReportResponse fromDb(DBReport report) {
    return new ReportResponse(report.id, report.content);
  }

}
