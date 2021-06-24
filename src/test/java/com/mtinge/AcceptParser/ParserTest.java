package com.mtinge.AcceptParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParserTest {
  @Test
  public void parsesMimesWithoutWeight() {
    var result = Parser.parse("a/b");
    var entries = result.getEntries();

    Assertions.assertEquals(1, entries.length);
    Assertions.assertEquals("a", entries[0].getMime().getType());
    Assertions.assertEquals("b", entries[0].getMime().getSubtype());
    Assertions.assertEquals(0D, entries[0].getWeight());
  }

  @Test
  public void parsesMimesWithWeight() {
    var result = Parser.parse("a/b;q=0.3");
    var entries = result.getEntries();

    Assertions.assertEquals(1, entries.length);
    Assertions.assertEquals("a", entries[0].getMime().getType());
    Assertions.assertEquals("b", entries[0].getMime().getSubtype());
    Assertions.assertEquals(0.3, entries[0].getWeight());
  }

  @Test
  public void parsesMultipleMimes() {
    var result = Parser.parse("a/b, y/z;q=0.3");
    var entries = result.getEntries();

    Assertions.assertEquals(2, entries.length);
    Assertions.assertEquals("a", entries[0].getMime().getType());
    Assertions.assertEquals("b", entries[0].getMime().getSubtype());
    Assertions.assertEquals(0D, entries[0].getWeight());

    Assertions.assertEquals("y", entries[1].getMime().getType());
    Assertions.assertEquals("z", entries[1].getMime().getSubtype());
    Assertions.assertEquals(0.3D, entries[1].getWeight());
  }

  @Test
  public void findsCorrectHeaviest() {
    var result = Parser.parse("application/json;q=1.0, text/xml;q=0.9");
    Assertions.assertNotNull(result);
    Assertions.assertNotNull(result.getHeaviest());
    Assertions.assertEquals(new Mime("application", "json"), result.getHeaviest().getMime());
  }

  @Test
  public void findsCorrectBestMatch() {
    var result = Parser.parse("application/json;q=1.0, text/xml;q=0.9; text/html;q=0.8; */*");

    var searchA = Mime.parse("text/xml");
    var searchB = Mime.parse("text/html");
    var match = result.bestMatch(searchA, searchB);
    Assertions.assertNotNull(match);
    Assertions.assertEquals(searchA, match.getMime());
  }
}
