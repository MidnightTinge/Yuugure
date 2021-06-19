package com.mtinge.yuugure.core;

@SuppressWarnings("ALL")
public final class States {
  // @formatter:off
  public static final class Account {
    public static final long DEACTIVATED         = 1L << 0L; // 0b1
    public static final long DELETED             = 1L << 1L; // 0b10
    public static final long BANNED              = 1L << 2L; // 0b100...
    public static final long UPLOAD_RESTRICTED   = 1L << 3L;
    public static final long COMMENTS_RESTRICTED = 1L << 4L;
    public static final long TRUSTED_UPLOADS     = 1L << 5L;
    public static final long PRIVATE             = 1L << 6L;
  }

  public static final class Upload {
    public static final long PRIVATE           = 1L << 0L; // 0b1
    public static final long DELETED           = 1L << 1L; // 0b10
    public static final long DMCA              = 1L << 2L; // 0b100...
    public static final long LOCKED_TAGS       = 1L << 3L;
    public static final long LOCKED_COMMENTS   = 1L << 4L;
    public static final long MODERATION_QUEUED = 1L << 5L;
  }
  // @formatter:on

  /**
   * Computes a state bitfield starting from 0L
   *
   * @param states The states to flag
   *
   * @return The computed state
   */
  public static long compute(long... states) {
    long ret = 0L;
    for (var flag : states) {
      ret |= flag;
    }

    return ret;
  }

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
   * @param toAdd The flag(s) to set 'on'.
   *
   * @return The newly computed state bitfield with the requested flag set to 'on'.
   */
  public static long addFlag(long curState, long... toAdd) {
    long ret = curState;
    for (var flag : toAdd) {
      ret |= flag;
    }
    return ret;
  }

  /**
   * Computes a new bitfield with the requested flag set to 'off'.
   *
   * @param curState The current fully computed state bitfield.
   * @param toRemove The flag to set 'off'.
   *
   * @return The newly computed state bitfield with the requested flag set to 'off'.
   */
  public static long removeFlag(long curState, long... toRemove) {
    long ret = curState;
    for (var flag : toRemove) {
      ret &= ~flag;
    }
    return ret;
  }
}
