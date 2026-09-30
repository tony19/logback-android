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
package ch.qos.logback.classic.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.qos.logback.classic.LoggerContext;

public class UrlCssBuilderTest {

  @Test
  public void defaultsToLogbackClassicStylesheet() {
    UrlCssBuilder builder = new UrlCssBuilder();
    assertEquals("http://logback.qos.ch/css/classic.css", builder.getUrl());

    StringBuilder sb = new StringBuilder("<head>");
    builder.addCss(sb);
    assertEquals("<head><link REL=StyleSheet HREF=\"http://logback.qos.ch/css/classic.css\""
        + " TITLE=\"Basic\" />", sb.toString());
  }

  @Test
  public void linksToConfiguredUrl() {
    UrlCssBuilder builder = new UrlCssBuilder();
    builder.setUrl("https://example.invalid/my.css");
    assertEquals("https://example.invalid/my.css", builder.getUrl());

    StringBuilder sb = new StringBuilder();
    builder.addCss(sb);
    assertEquals("<link REL=StyleSheet HREF=\"https://example.invalid/my.css\" TITLE=\"Basic\" />",
        sb.toString());
  }

  @Test
  public void htmlLayoutFileHeaderUsesTheLinkInsteadOfInlineStyles() {
    UrlCssBuilder builder = new UrlCssBuilder();
    builder.setUrl("my.css");
    HTMLLayout layout = new HTMLLayout();
    layout.setContext(new LoggerContext());
    layout.setCssBuilder(builder);

    String header = layout.getFileHeader();

    assertTrue(header, header.contains("<link REL=StyleSheet HREF=\"my.css\" TITLE=\"Basic\" />"));
    assertFalse(header, header.contains("<style"));
  }
}
