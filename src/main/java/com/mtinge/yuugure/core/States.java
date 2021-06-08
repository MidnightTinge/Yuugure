package com.mtinge.yuugure.core;

@SuppressWarnings("ALL")
public final class States {
  // @formatter:off
  public static final class User {
    public static final long DEACTIVATED       = 1L << 0L; // 0b1
    public static final long DELETED           = 1L << 1L; // 0b10
    public static final long BANNED            = 1L << 2L; // 0b100...
    public static final long UPLOAD_RESTRICTED = 1L << 3L;
  }
  // @formatter:on

  /**
   * Checks if the given state has the requested flag.
   *
   * @param curState The fully computed state bitfield.
   * @param toCheck The flag to check.
   *
   * @return Whether or not the computed state bitfield contains the requested flag.
   */
  public static boolean flagged(long curState, long toCheck) {
    return (toCheck & curState) > 0;
  }

  /**
   * Computes a new bitfield with the requested flag set to 'on'.
   *
   * @param curState The current fully computed state bitfield.
   * @param toAdd The flag to set 'on'.
   *
   * @return The newly computed state bitfield with the requested flag set to 'on'.
   */
  public static long addFlag(long curState, long toAdd) {
    return (curState | toAdd);
  }

  /**
   * Computes a new bitfield with the requested flag set to 'off'.
   *
   * @param curState The current fully computed state bitfield.
   * @param toRemove The flag to set 'off'.
   *
   * @return The newly computed state bitfield with the requested flag set to 'off'.
   */
  public static long removeFlag(long curState, long toRemove) {
    return curState & ~toRemove;
  }
}
