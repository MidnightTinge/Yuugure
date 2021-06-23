package com.mtinge.yuugure.core.comments;

//attributes.put("href", "/leaving?url=" + URLEncoder.encode(attributes.get("href"), StandardCharsets.UTF_8));

import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

import java.util.Collections;
import java.util.Set;

public class LinkHrefRenderer implements NodeRenderer {
  private final HtmlWriter html;

  LinkHrefRenderer(HtmlNodeRendererContext context) {
    this.html = context.getWriter();
  }

  @Override
  public Set<Class<? extends Node>> getNodeTypes() {
    return Collections.singleton(Link.class);
  }

  @Override
  public void render(Node node) {
    html.tag("span");
    html.text(((Link) node).getDestination());
    html.tag("/span");
  }
}
