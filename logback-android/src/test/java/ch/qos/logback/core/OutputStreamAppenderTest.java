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
package ch.qos.logback.core;


import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.parser.SamplePatternLayout;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class OutputStreamAppenderTest {

  Context context = new ContextBase();
  StatusChecker statusChecker = new StatusChecker(context);
  
  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void smoke() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER = "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }

  @Test
  public void nullFileHeader() {
    String FILE_HEADER = null;
    String PRESENTATION_HEADER = "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }

  @Test
  public void nullPresentationHeader() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER = null;
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }

  @Test
  public void nullPresentationFooter() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER =  "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = null;
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }
  
  @Test
  public void nullFileFooter() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER = "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = null;
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }
  
  public void headerFooterCheck(String fileHeader, String presentationHeader, String presentationFooter, String fileFooter) {
    OutputStreamAppender<Object> wa = new OutputStreamAppender<Object>();
    wa.setContext(context);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
 
    SamplePatternLayout<Object> spl = new SamplePatternLayout<Object>();
    spl.setContext(context);
  
    spl.setFileHeader(fileHeader);
    spl.setPresentationHeader(presentationHeader);
    spl.setPresentationFooter(presentationFooter);
    spl.setFileFooter(fileFooter);
  
    spl.start();
    LayoutWrappingEncoder<Object> encoder = new LayoutWrappingEncoder<Object>();
    encoder.setLayout(spl);
    encoder.setContext(context);
    
    wa.setEncoder(encoder);
    wa.setOutputStream(baos);
    wa.start();
    
    wa.stop();
    String result = baos.toString();

    String expectedHeader = emtptyIfNull(fileHeader) + emtptyIfNull(presentationHeader);

    System.out.println(result);
    assertTrue(result, result.startsWith(expectedHeader));

    String expectedFooter = emtptyIfNull(presentationFooter) + emtptyIfNull(fileFooter);
    assertTrue(result, result.endsWith(expectedFooter));
  }
  
  String emtptyIfNull(String s) {
    return s == null ? "" : s;
  }

  @Test
  public void eventsAreWrittenThroughWriteOut() {
    final List<Object> writtenOut = new ArrayList<Object>();
    OutputStreamAppender<Object> appender = new OutputStreamAppender<Object>() {
      @Override
      protected void writeOut(Object event) throws IOException {
        writtenOut.add(event);
        super.writeOut(event);
      }
    };
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    appender.setContext(context);
    appender.setEncoder(new Utf8LineEncoder());
    appender.setOutputStream(baos);
    appender.start();

    appender.doAppend("a");
    appender.doAppend("b");

    assertEquals(Arrays.<Object>asList("a", "b"), writtenOut);
    assertEquals("a\nb\n", utf8(baos));
  }

  @Test
  public void startWithoutEncoderReportsErrorAndDoesNotStart() {
    OutputStreamAppender<Object> appender = newAppender(null);
    appender.setOutputStream(new ByteArrayOutputStream());
    statusChecker.assertContainsMatch(Status.WARN, "Encoder has not been set. Cannot invoke its init method.");

    appender.start();

    assertFalse(appender.isStarted());
    statusChecker.assertContainsMatch(Status.ERROR, "No encoder set for the appender named \"osa\".");
    statusChecker.assertNoMatch("No output stream set");
  }

  @Test
  public void startWithoutOutputStreamReportsErrorAndDoesNotStart() {
    OutputStreamAppender<Object> appender = newAppender(new Utf8LineEncoder());

    appender.start();

    assertFalse(appender.isStarted());
    statusChecker.assertContainsMatch(Status.ERROR, "No output stream set for the appender named \"osa\".");
    statusChecker.assertNoMatch("No encoder set");
  }

  @Test
  public void startWithoutEncoderAndOutputStreamReportsBothErrors() {
    OutputStreamAppender<Object> appender = newAppender(null);

    appender.start();

    assertFalse(appender.isStarted());
    assertEquals(2, statusChecker.matchCount("No (encoder|output stream) set for the appender named \"osa\"."));
  }

  @Test
  public void appendOnNonStartedAppenderWritesNothing() {
    OutputStreamAppender<Object> appender = newAppender(new Utf8LineEncoder());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    appender.setOutputStream(baos);

    appender.append("ignored");

    assertEquals("", utf8(baos));
  }

  @Test
  public void subAppendOnNonStartedAppenderWritesNothing() {
    OutputStreamAppender<Object> appender = newAppender(new Utf8LineEncoder());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    appender.setOutputStream(baos);

    appender.subAppend("ignored");

    assertEquals("", utf8(baos));
  }

  @Test
  public void everyEventIsFlushedByDefault() {
    OutputStreamAppender<Object> appender = newAppender(new Utf8LineEncoder());
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();
    appender.setOutputStream(out);
    appender.start();
    assertTrue(appender.isImmediateFlush());

    appender.doAppend("a");
    appender.doAppend("b");

    assertEquals("a\nb\n", out.text());
    assertEquals(2, out.flushCount);
  }

  @Test
  public void eventsAreNotFlushedWhenImmediateFlushIsOff() {
    OutputStreamAppender<Object> appender = newAppender(new Utf8LineEncoder());
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();
    appender.setOutputStream(out);
    appender.setImmediateFlush(false);
    appender.start();

    appender.doAppend("a");

    assertFalse(appender.isImmediateFlush());
    assertEquals("a\n", out.text());
    assertEquals(0, out.flushCount);
  }

  @Test
  public void writeFailureStopsAppenderAndReportsError() {
    OutputStreamAppender<Object> appender = newAppender(new Utf8LineEncoder());
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();
    appender.setOutputStream(out);
    appender.start();
    out.failWrites = true;

    appender.doAppend("lost");

    assertFalse(appender.isStarted());
    statusChecker.assertContainsMatch(Status.ERROR, "IO failure in appender");
    statusChecker.asssertContainsException(IOException.class);
  }

  @Test
  public void headerWriteFailureStopsAppenderAndReportsError() {
    Utf8LineEncoder encoder = new Utf8LineEncoder();
    encoder.header = Utf8LineEncoder.toUtf8("header\n");
    OutputStreamAppender<Object> appender = newAppender(encoder);
    appender.setOutputStream(new ByteArrayOutputStream());
    appender.start();
    assertTrue(appender.isStarted());

    FaultInjectingOutputStream second = new FaultInjectingOutputStream();
    second.failWrites = true;
    appender.setOutputStream(second);

    assertFalse(appender.isStarted());
    assertSame(second, appender.getOutputStream());
    statusChecker.assertContainsMatch(Status.ERROR, "Failed to initialize encoder for appender named \\[osa\\].");
    statusChecker.asssertContainsException(IOException.class);
  }

  @Test
  public void footerWriteFailureStopsAppenderAndReportsError() {
    Utf8LineEncoder encoder = new Utf8LineEncoder();
    encoder.footer = Utf8LineEncoder.toUtf8("footer\n");
    OutputStreamAppender<Object> appender = newAppender(encoder);
    FaultInjectingOutputStream first = new FaultInjectingOutputStream();
    appender.setOutputStream(first);
    appender.start();
    first.failWrites = true;

    // replacing the stream closes the previous one, writing the footer first
    ByteArrayOutputStream second = new ByteArrayOutputStream();
    appender.setOutputStream(second);

    assertFalse(appender.isStarted());
    assertTrue(first.closed);
    assertSame(second, appender.getOutputStream());
    statusChecker.assertContainsMatch(Status.ERROR, "Failed to write footer for appender named \\[osa\\].");
  }

  @Test
  public void closeFailureIsReportedOnStop() {
    OutputStreamAppender<Object> appender = newAppender(new Utf8LineEncoder());
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();
    out.failClose = true;
    appender.setOutputStream(out);
    appender.start();

    appender.stop();

    assertFalse(appender.isStarted());
    statusChecker.assertContainsMatch(Status.ERROR, "Could not close output stream for OutputStreamAppender.");
    statusChecker.asssertContainsException(IOException.class);
  }

  @Test
  public void stopWithoutEncoderClosesStreamWithoutFooter() {
    OutputStreamAppender<Object> appender = newAppender(null);
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();
    appender.setOutputStream(out);

    appender.stop();

    assertTrue(out.closed);
    assertEquals("", out.text());
    assertNull(appender.getOutputStream());
    statusChecker.assertIsErrorFree();
  }

  @Test
  public void settingNullOutputStreamClosesPreviousStreamAndWritesNoHeader() {
    Utf8LineEncoder encoder = new Utf8LineEncoder();
    encoder.header = Utf8LineEncoder.toUtf8("header\n");
    encoder.footer = Utf8LineEncoder.toUtf8("footer\n");
    OutputStreamAppender<Object> appender = newAppender(encoder);
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();
    appender.setOutputStream(out);

    appender.setOutputStream(null);

    assertNull(appender.getOutputStream());
    assertTrue(out.closed);
    assertEquals("header\nfooter\n", out.text());
    statusChecker.assertIsErrorFree();
  }

  @Test
  public void emptyHeaderWritesNothing() {
    Utf8LineEncoder encoder = new Utf8LineEncoder();
    encoder.header = new byte[0];
    OutputStreamAppender<Object> appender = newAppender(encoder);
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();

    appender.setOutputStream(out);

    assertEquals("", out.text());
    assertEquals(0, out.flushCount);
  }

  @Test
  public void encoderInitWithoutEncoderWritesNothing() {
    OutputStreamAppender<Object> appender = newAppender(null);
    FaultInjectingOutputStream out = new FaultInjectingOutputStream();
    appender.setOutputStream(out);

    appender.encoderInit();

    assertEquals("", out.text());
    statusChecker.assertIsErrorFree();
  }

  @Test
  public void encoderCloseWithoutOutputStreamDoesNothing() {
    Utf8LineEncoder encoder = new Utf8LineEncoder();
    encoder.footer = Utf8LineEncoder.toUtf8("footer\n");
    OutputStreamAppender<Object> appender = newAppender(encoder);

    appender.encoderClose();

    assertNull(appender.getOutputStream());
    statusChecker.assertIsErrorFree();
  }

  private OutputStreamAppender<Object> newAppender(Utf8LineEncoder encoder) {
    OutputStreamAppender<Object> appender = new OutputStreamAppender<Object>();
    appender.setContext(context);
    appender.setName("osa");
    appender.setEncoder(encoder);
    return appender;
  }

  static String utf8(ByteArrayOutputStream baos) {
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  /**
   * Encodes each event as its string form in UTF-8 followed by a newline.
   */
  static class Utf8LineEncoder extends EncoderBase<Object> {
    byte[] header;
    byte[] footer;

    @Override
    public byte[] headerBytes() {
      return header;
    }

    @Override
    public byte[] encode(Object event) {
      return toUtf8(event + "\n");
    }

    @Override
    public byte[] footerBytes() {
      return footer;
    }

    static byte[] toUtf8(String s) {
      return s.getBytes(StandardCharsets.UTF_8);
    }
  }

  /**
   * Records what is written and flushed, and throws IOException from
   * write()/close() on demand.
   */
  static class FaultInjectingOutputStream extends OutputStream {
    final ByteArrayOutputStream written = new ByteArrayOutputStream();
    boolean failWrites;
    boolean failClose;
    boolean closed;
    int flushCount;

    @Override
    public void write(int b) throws IOException {
      if (failWrites) {
        throw new IOException("write failed");
      }
      written.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      if (failWrites) {
        throw new IOException("write failed");
      }
      written.write(b, off, len);
    }

    @Override
    public void flush() {
      flushCount++;
    }

    @Override
    public void close() throws IOException {
      if (failClose) {
        throw new IOException("close failed");
      }
      closed = true;
    }

    String text() {
      return utf8(written);
    }
  }
}
