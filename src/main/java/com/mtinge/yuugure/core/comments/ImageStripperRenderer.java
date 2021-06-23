package com.mtinge.yuugure.core.comments;

import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Collections;
import java.util.Set;

public class ImageStripperRenderer implements NodeRenderer {
  private final HtmlWriter html;

  ImageStripperRenderer(HtmlNodeRendererContext context) {
    this.html = context.getWriter();
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Collections.singleton(Image.class);
  }

  @Override
  public void render(Node node) {
    // don't render anything
  }
}
