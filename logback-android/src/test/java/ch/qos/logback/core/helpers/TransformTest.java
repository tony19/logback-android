/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.helpers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class TransformTest {

  @Test
  public void publicNoArgConstructorIsAvailable() throws Exception {
    // the class only has static methods, but its implicit public constructor is part of its API
    assertNotNull(Transform.class.getConstructor().newInstance());
  }

  @Test
  public void escapeTagsReturnsNullForNull() {
    assertNull(Transform.escapeTags((String) null));
  }

  @Test
  public void escapeTagsReturnsEmptyStringAsIs() {
    String empty = new String("");
    assertSame(empty, Transform.escapeTags(empty));
  }

  @Test
  public void escapeTagsReturnsSafeStringAsIs() {
    String safe = "plain text\twith\ttabs\r\nand line breaks";
    assertSame(safe, Transform.escapeTags(safe));
  }

  @Test
  public void escapeTagsReplacesMarkupCharactersWithEntities() {
    assertEquals("&lt;a href=&quot;x&quot; title=&#39;y&#39;&gt;&amp;&lt;/a&gt;",
        Transform.escapeTags("<a href=\"x\" title='y'>&</a>"));
  }

  @Test
  public void escapeTagsKeepsTabsAndLineBreaksNextToEscapedCharacters() {
    assertEquals("\t&lt;\n&gt;\r&amp;", Transform.escapeTags("\t<\n>\r&"));
  }

  @Test
  public void escapeTagsReplacesOtherControlCharactersWithReplacementCharacter() {
    assertEquals("a\uFFFDb\uFFFDc\uFFFD d", Transform.escapeTags("a\u0000b\u000bc\u001f d"));
  }

  @Test
  public void escapeTagsOfStringBufferEscapesTheBufferInPlace() {
    StringBuffer buf = new StringBuffer("x<\u0001y");

    String result = Transform.escapeTags(buf);

    assertEquals("x&lt;\uFFFDy", result);
    assertEquals("x&lt;\uFFFDy", buf.toString());
  }

  @Test
  public void escapeTagsOfStringBufferWithoutSpecialCharactersLeavesItUnchanged() {
    StringBuffer buf = new StringBuffer("a b\tc");
    assertEquals("a b\tc", Transform.escapeTags(buf));
  }

  @Test
  public void appendEscapingCDATAIgnoresNull() {
    StringBuilder output = new StringBuilder("pre");
    Transform.appendEscapingCDATA(output, null);
    assertEquals("pre", output.toString());
  }

  @Test
  public void appendEscapingCDATAAppendsStringWithoutCDATAEndAsIs() {
    StringBuilder output = new StringBuilder("pre");
    Transform.appendEscapingCDATA(output, "a]]b>c<");
    assertEquals("prea]]b>c<", output.toString());
  }

  @Test
  public void appendEscapingCDATASplitsTheSectionAroundAnEmbeddedCDATAEnd() {
    StringBuilder output = new StringBuilder();
    Transform.appendEscapingCDATA(output, "a]]>b");
    assertEquals("a]]>]]&gt;<![CDATA[b", output.toString());
  }

  @Test
  public void appendEscapingCDATAHandlesEveryEmbeddedCDATAEnd() {
    StringBuilder output = new StringBuilder();
    Transform.appendEscapingCDATA(output, "]]>x]]>y");
    assertEquals("]]>]]&gt;<![CDATA[x]]>]]&gt;<![CDATA[y", output.toString());
  }

  @Test
  public void appendEscapingCDATAHandlesTrailingCDATAEnd() {
    StringBuilder output = new StringBuilder();
    Transform.appendEscapingCDATA(output, "a]]>");
    assertEquals("a]]>]]&gt;<![CDATA[", output.toString());
  }
}
