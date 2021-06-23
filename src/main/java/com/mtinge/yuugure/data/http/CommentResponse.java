package com.mtinge.yuugure.data.http;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class CommentResponse extends InputAwareResponse {
  private RenderableComment comment;

  public CommentResponse() {
    super();
  }
}
