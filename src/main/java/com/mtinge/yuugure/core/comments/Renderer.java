package com.mtinge.yuugure.core.comments;


import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.*;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Renderer {
  private static final CharSequenceTranslator translator = new LookupTranslator(
    Map.of(
      "\"", "&quot;",
      "&", "&amp;",
      "<", "&lt;",
      ">", "&gt;"
    )
  );
  private static final Function<HtmlStreamEventReceiver, HtmlSanitizer.Policy> POLICY = new HtmlPolicyBuilder()
    .allowStandardUrlProtocols()
    .allowAttributes("title", "class").globally()
    .allowAttributes("target").matching(Pattern.compile("_blank")).onElements("a")
    .allowAttributes("href").onElements("a")
    .requireRelNofollowOnLinks()
    .allowCommonBlockElements()
    .allowCommonInlineFormattingElements()
    .allowElements(
      "a", "p", "div", "span", "i", "b", "em", "blockquote", "tt", "strong", "br", "ul", "ol", "li"
    ).toFactory();
  private static final Parser parser = Parser.builder().build();
  private static final HtmlRenderer renderer = HtmlRenderer.builder().nodeRendererFactory(ImageStripperRenderer::new).nodeRendererFactory(LinkHrefRenderer::new).build();

  public static String markdown(String input) {
    var document = parser.parse(input);
    return renderer.render(document);
  }

  public static String escapeTags(String input) {
    return translator.translate(input);
  }

  public static String sanitize(String input) {
    var builder = new StringBuilder();
    HtmlSanitizer.sanitize(input, POLICY.apply(HtmlStreamRenderer.create(builder, Handler.DO_NOTHING)));

    return builder.toString();
  }

  public static String render(String comment) {
    /*
     * Strip tags for basic sanitization,
     * Parse any markdown,
     * Sanitize the resulting HTML again using OWSAP,
     * Store in the database.
     */

    String result = Renderer.escapeTags(comment);
    result = Renderer.markdown(result);
    result = Renderer.sanitize(result);

    return result.trim();
  }
}
