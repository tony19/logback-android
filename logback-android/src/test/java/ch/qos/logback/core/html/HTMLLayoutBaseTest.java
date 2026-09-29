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
package ch.qos.logback.core.html;

import static ch.qos.logback.core.CoreConstants.LINE_SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.LiteralConverter;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class HTMLLayoutBaseTest {

  /** Named with the "Converter" suffix, so its column is called "Shout". */
  public static class ShoutConverter extends DynamicConverter<Object> {
    @Override
    public String convert(Object event) {
      return String.valueOf(event).toUpperCase(Locale.US);
    }
  }

  /** Named without the "Converter" suffix, so its column keeps the full simple name. */
  public static class Echo extends DynamicConverter<Object> {
    @Override
    public String convert(Object event) {
      return String.valueOf(event);
    }
  }

  static class TestHTMLLayout extends HTMLLayoutBase<Object> {
    Map<String, String> defaultConverterMap = new HashMap<String, String>();
    boolean hideLiteralColumns;

    TestHTMLLayout() {
      defaultConverterMap.put("shout", ShoutConverter.class.getName());
      defaultConverterMap.put("echo", Echo.class.getName());
    }

    @Override
    protected Map<String, String> getDefaultConverterMap() {
      return defaultConverterMap;
    }

    @Override
    protected String computeConverterName(Converter<Object> c) {
      if (hideLiteralColumns && c instanceof LiteralConverter) {
        return null;
      }
      return super.computeConverterName(c);
    }

    @Override
    public String doLayout(Object event) {
      StringBuilder sb = new StringBuilder();
      startNewTableIfLimitReached(sb);
      counter++;
      for (Converter<Object> c = head; c != null; c = c.getNext()) {
        sb.append("<td>").append(c.convert(event)).append("</td>");
      }
      return sb.toString();
    }
  }

  private static final String SHOUT_ECHO_HEADER_ROW = "<tr class=\"header\">" + LINE_SEPARATOR
      + "<td class=\"Shout\">Shout</td>" + LINE_SEPARATOR
      + "<td class=\"Echo\">Echo</td>" + LINE_SEPARATOR
      + "</tr>" + LINE_SEPARATOR;

  private Context context;
  private TestHTMLLayout layout;

  @Before
  public void setUp() {
    context = new ContextBase();
    layout = new TestHTMLLayout();
    layout.setContext(context);
  }

  @Test
  public void patternIsSettable() {
    layout.setPattern("%shout");
    assertEquals("%shout", layout.getPattern());
  }

  @Test
  public void titleDefaultsToLogbackLogMessagesAndIsWrittenIntoTheFileHeader() {
    assertEquals("Logback Log Messages", layout.getTitle());
    layout.setTitle("My <b>app</b>");
    assertEquals("My <b>app</b>", layout.getTitle());
    layout.setCssBuilder(new CssBuilder() {
      @Override
      public void addCss(StringBuilder sbuf) {
        sbuf.append("<css/>");
      }
    });

    assertEquals("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\""
        + " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" + LINE_SEPARATOR
        + "<html>" + LINE_SEPARATOR
        + "  <head>" + LINE_SEPARATOR
        + "    <title>My <b>app</b></title>" + LINE_SEPARATOR
        + "<css/>" + LINE_SEPARATOR
        + "  </head>" + LINE_SEPARATOR
        + "<body>" + LINE_SEPARATOR, layout.getFileHeader());
  }

  @Test
  public void cssBuilderIsSettable() {
    CssBuilder css = new CssBuilder() {
      @Override
      public void addCss(StringBuilder sbuf) {
      }
    };
    layout.setCssBuilder(css);
    assertSame(css, layout.getCssBuilder());
  }

  @Test
  public void contentTypeIsTextHtml() {
    assertEquals("text/html", layout.getContentType());
  }

  @Test
  public void footersCloseTheTableAndTheDocument() {
    assertEquals("</table>", layout.getPresentationFooter());
    assertEquals(LINE_SEPARATOR + "</body></html>", layout.getFileFooter());
  }

  @Test
  public void columnsAreNamedAfterConverterClassWithoutTheConverterSuffix() {
    layout.setPattern("%shout%echo");
    layout.start();

    assertTrue(layout.isStarted());
    String header = layout.getPresentationHeader();
    assertTrue(header, header.endsWith("<table cellspacing=\"0\">" + LINE_SEPARATOR
        + SHOUT_ECHO_HEADER_ROW));
    assertEquals("<td>HI</td><td>hi</td>", layout.doLayout("hi"));
  }

  @Test
  public void headerRowSkipsConvertersWhoseNameIsNull() {
    layout.hideLiteralColumns = true;
    layout.setPattern("%shout - %echo");
    layout.start();

    String header = layout.getPresentationHeader();
    assertTrue(header, header.endsWith(SHOUT_ECHO_HEADER_ROW));
    assertFalse(header, header.contains("Literal"));
  }

  @Test
  public void headerRowListsLiteralConvertersWhenTheyHaveAName() {
    layout.setPattern("%shout - %echo");
    layout.start();

    assertTrue(layout.getPresentationHeader().contains("<td class=\"Literal\">Literal</td>"));
  }

  @Test
  public void incorrectPatternIsReportedAndLayoutDoesNotStart() {
    layout.setPattern("hello%(abc");
    layout.start();

    assertFalse(layout.isStarted());
    StatusChecker checker = new StatusChecker(context);
    checker.assertContainsMatch(Status.ERROR, "Incorrect pattern found");
    checker.asssertContainsException(ScanException.class);
  }

  @Test
  public void reachingTheRowLimitStartsNewTableWithHeaderRowAndResetsCounter() {
    layout.setPattern("%shout%echo");
    layout.start();
    layout.counter = CoreConstants.TABLE_ROW_LIMIT;

    StringBuilder sb = new StringBuilder();
    layout.startNewTableIfLimitReached(sb);

    assertEquals("</table>" + LINE_SEPARATOR
        + "<p></p><table cellspacing=\"0\">" + LINE_SEPARATOR
        + SHOUT_ECHO_HEADER_ROW, sb.toString());
    assertEquals(0, layout.counter);
  }

  @Test
  public void belowTheRowLimitNoNewTableIsStarted() {
    layout.setPattern("%shout");
    layout.start();
    layout.counter = CoreConstants.TABLE_ROW_LIMIT - 1;

    StringBuilder sb = new StringBuilder();
    layout.startNewTableIfLimitReached(sb);

    assertEquals("", sb.toString());
    assertEquals(CoreConstants.TABLE_ROW_LIMIT - 1, layout.counter);
  }

  @Test
  public void rowAfterTheLimitIsPrecededByANewTable() {
    layout.setPattern("%echo");
    layout.start();
    layout.counter = CoreConstants.TABLE_ROW_LIMIT - 1;

    assertEquals("<td>last</td>", layout.doLayout("last"));
    String next = layout.doLayout("next");

    assertTrue(next, next.startsWith("</table>"));
    assertTrue(next, next.endsWith("<td>next</td>"));
    assertEquals(1, layout.counter);
  }

  @Test
  public void contextConversionRulesOverrideDefaultConverters() {
    context.putObject(CoreConstants.PATTERN_RULE_REGISTRY,
        Collections.singletonMap("shout", Echo.class.getName()));

    Map<String, String> effective = layout.getEffectiveConverterMap();

    assertEquals(2, effective.size());
    assertEquals(Echo.class.getName(), effective.get("shout"));
    assertEquals(Echo.class.getName(), effective.get("echo"));
  }

  @Test
  public void effectiveConverterMapWithoutDefaultsHoldsOnlyContextRules() {
    layout.defaultConverterMap = null;
    context.putObject(CoreConstants.PATTERN_RULE_REGISTRY,
        Collections.singletonMap("echo", Echo.class.getName()));

    assertEquals(Collections.singletonMap("echo", Echo.class.getName()),
        layout.getEffectiveConverterMap());
  }

  @Test
  public void effectiveConverterMapWithoutContextHoldsOnlyDefaults() {
    TestHTMLLayout contextless = new TestHTMLLayout();

    Map<String, String> effective = contextless.getEffectiveConverterMap();

    assertEquals(contextless.defaultConverterMap, effective);
  }

  @Test
  public void effectiveConverterMapIsEmptyWithoutDefaultsOrContextRules() {
    layout.defaultConverterMap = null;

    assertTrue(layout.getEffectiveConverterMap().isEmpty());
  }
}
