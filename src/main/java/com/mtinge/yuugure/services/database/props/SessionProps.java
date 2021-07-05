package com.mtinge.yuugure.services.database.props;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public class SessionProps {
  /**
   * The token that identifies this session.
   */
  private String token = null;
  /**
   * When this session expires.
   */
  private Instant expires = null;
  /**
   * The account ID this session identifies.
   */
  private Integer account = null;
}
