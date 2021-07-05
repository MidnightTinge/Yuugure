package com.mtinge.yuugure.services.database.props;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
public final class AccountProps {
  /**
   * The user's email.
   */
  private String email = null;
  /**
   * The user's username.
   */
  private String username = null;
  /**
   * The user's non-hashed password.
   */
  private String password = null;
  /**
   * The user's computed state.
   *
   * @see com.mtinge.yuugure.core.States.Account
   */
  private Long state = null;
}
