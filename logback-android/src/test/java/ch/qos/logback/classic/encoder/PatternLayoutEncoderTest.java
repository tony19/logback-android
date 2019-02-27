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
package ch.qos.logback.classic.encoder;

import static junit.framework.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import ch.qos.logback.classic.PatternLayout;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

public class PatternLayoutEncoderTest {

  PatternLayoutEncoder ple = new PatternLayoutEncoder();
  LoggerContext context = new LoggerContext();
  ByteArrayOutputStream baos = new ByteArrayOutputStream();
  Logger logger = context.getLogger(PatternLayoutEncoderTest.class);
  Charset utf8Charset = Charset.forName("UTF-8");
  
  @Before
  public void setUp() {
    ple.setPattern("%m");
    ple.setContext(context);
  }

  ILoggingEvent makeLoggingEvent(String message) {
    return new LoggingEvent("", logger, Level.DEBUG, message, null, null);
  }

  @Test
  public void smoke() throws IOException {
    init(baos);
    String msg = "hello";
    ILoggingEvent event = makeLoggingEvent(msg);
    byte[] eventBytes = ple.encode(event);
    baos.write(eventBytes);
    ple.footerBytes();
    assertEquals(msg, baos.toString());
  }

  void init(ByteArrayOutputStream baos) throws IOException {
    ple.start();
    ((PatternLayout) ple.getLayout()).setOutputPatternAsHeader(false);
    byte[] header = ple.headerBytes();
    baos.write(header);
  }

  @Test
  public void charset() throws IOException {
    ple.setCharset(utf8Charset);
    init(baos);
    String msg = "\u03b1";
    ILoggingEvent event = makeLoggingEvent(msg);
    byte[] eventBytes = ple.encode(event);
    baos.write(eventBytes);
    ple.footerBytes();
    assertEquals(msg, new String(baos.toByteArray(), utf8Charset));
  }

}
