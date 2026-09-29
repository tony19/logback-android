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
package ch.qos.logback.core.encoder;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.layout.DummyLayout;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LayoutWrappingEncoderTest {

  private static final Charset UTF_16BE = Charset.forName("UTF-16BE");

  private final Context context = new ContextBase();
  private final StatusChecker checker = new StatusChecker(context);
  private final LayoutWrappingEncoder<Object> encoder = new LayoutWrappingEncoder<Object>();
  private final DummyLayout<Object> layout = new DummyLayout<Object>("body");

  @Before
  public void setUp() {
    encoder.setContext(context);
    layout.setContext(context);
  }

  @Test
  public void headerBytesIsNullWithoutLayout() {
    assertNull(encoder.getLayout());
    assertNull(encoder.headerBytes());
  }

  @Test
  public void footerBytesIsNullWithoutLayout() {
    assertNull(encoder.footerBytes());
  }

  @Test
  public void headerBytesJoinsFileAndPresentationHeaderAndEndsWithLineSeparator() {
    layout.setFileHeader("FH");
    layout.setPresentationHeader("PH");
    encoder.setLayout(layout);
    encoder.setCharset(UTF_16BE);

    assertArrayEquals(("FHPH" + CoreConstants.LINE_SEPARATOR).getBytes(UTF_16BE), encoder.headerBytes());
  }

  @Test
  public void headerBytesIsEmptyWithoutLineSeparatorWhenLayoutHasNoHeaders() {
    encoder.setLayout(layout);

    assertEquals(0, encoder.headerBytes().length);
  }

  @Test
  public void footerBytesJoinsPresentationAndFileFooter() {
    layout.setPresentationFooter("PF");
    layout.setFileFooter("FF");
    encoder.setLayout(layout);
    encoder.setCharset(UTF_16BE);

    assertArrayEquals("PFFF".getBytes(UTF_16BE), encoder.footerBytes());
  }

  @Test
  public void encodeUsesConfiguredCharset() {
    encoder.setLayout(layout);
    encoder.setCharset(UTF_16BE);

    assertSame(UTF_16BE, encoder.getCharset());
    assertArrayEquals("body".getBytes(UTF_16BE), encoder.encode("ignored"));
  }

  @Test
  public void encodeWithoutCharsetUsesPlatformDefault() {
    encoder.setLayout(layout);

    assertNull(encoder.getCharset());
    assertArrayEquals("body".getBytes(), encoder.encode("ignored"));
  }

  @Test
  public void isStartedAlwaysReportsFalseAlthoughStartAndStopToggleTheFlag() {
    encoder.start();
    assertTrue(encoder.started);
    assertFalse(encoder.isStarted());

    encoder.stop();
    assertFalse(encoder.started);
    assertFalse(encoder.isStarted());
  }

  @Test
  public void startWithoutImmediateFlushLeavesParentUntouchedAndReportsNothing() {
    OutputStreamAppender<Object> parent = new OutputStreamAppender<Object>();
    parent.setImmediateFlush(false);
    encoder.setParent(parent);

    encoder.start();

    assertFalse(parent.isImmediateFlush());
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void setImmediateFlushWarnsThatThePropertyBelongsToTheAppender() {
    encoder.setImmediateFlush(false);

    checker.assertContainsMatch(Status.WARN, "As of version 1.2.0 \"immediateFlush\" property should be set within the enclosing Appender.");
    checker.assertContainsMatch(Status.WARN, "Please move \"immediateFlush\" property into the enclosing appender.");
  }

  @Test
  public void startPropagatesImmediateFlushToOutputStreamAppenderParent() {
    OutputStreamAppender<Object> parent = new OutputStreamAppender<Object>();
    assertTrue(parent.isImmediateFlush());
    encoder.setParent(parent);
    encoder.setImmediateFlush(false);

    encoder.start();

    assertFalse(parent.isImmediateFlush());
    assertTrue(encoder.started);
    checker.assertContainsMatch(Status.WARN, "Setting the \"immediateFlush\" property of the enclosing appender to false");
    checker.assertIsErrorFree();
  }

  @Test
  public void startReportsErrorWhenParentIsNotAnOutputStreamAppender() {
    encoder.setParent(new ListAppender<Object>());
    encoder.setImmediateFlush(true);

    encoder.start();

    assertTrue(encoder.started);
    checker.assertContainsMatch(Status.ERROR, "Could not set the \"immediateFlush\" property of the enclosing appender.");
    checker.assertNoMatch("Setting the \"immediateFlush\"");
  }

  @Test
  public void startReportsErrorWhenThereIsNoParent() {
    encoder.setImmediateFlush(true);

    encoder.start();

    assertTrue(encoder.started);
    checker.assertContainsMatch(Status.ERROR, "Could not set the \"immediateFlush\" property of the enclosing appender.");
  }
}
