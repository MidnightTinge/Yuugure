package com.mtinge.yuugure.core.TagManager;

public enum SearchMode {
  /**
   * Denotes a search in which the prefix is the most significant bit, e.g. "term*" <br />Can short
   * circuit
   */
  PREFIX,
  /**
   * Denotes a search in which the suffix is the most significant bit, e.g. "*term" <br />Can not
   * short circuit
   */
  SUFFIX,
  /**
   * Denotes a search in which the prefix and suffix are both anchors, e.g. "te*rm" <br />Can short
   * circuit
   */
  MIDDLE,
  /**
   * Denotes a search in which neither the prefix nor the suffix are the most significant bit, e.g.
   * "*term*" <br />Can not short circuit
   */
  WRAPPED,
}
