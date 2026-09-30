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
package ch.qos.logback.core.net;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SyslogAppenderBaseTest
{
  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private final Context context = new ContextBase();
  private final TestSyslogAppender appender = new TestSyslogAppender();

  @Before
  public void setUp() {
    appender.setContext(context);
    appender.setSyslogHost("localhost");
    appender.setFacility("USER");
    appender.setCharset(UTF_8);
  }

  @After
  public void tearDown() {
    appender.stop();
  }

  @Test
  public void testFacilityStringToint() throws InterruptedException
  {
    assertEquals(SyslogConstants.LOG_KERN, SyslogAppenderBase.facilityStringToint("KERN"));
    assertEquals(SyslogConstants.LOG_USER, SyslogAppenderBase.facilityStringToint("USER"));
    assertEquals(SyslogConstants.LOG_MAIL, SyslogAppenderBase.facilityStringToint("MAIL"));
    assertEquals(SyslogConstants.LOG_DAEMON, SyslogAppenderBase.facilityStringToint("DAEMON"));
    assertEquals(SyslogConstants.LOG_AUTH, SyslogAppenderBase.facilityStringToint("AUTH"));
    assertEquals(SyslogConstants.LOG_SYSLOG, SyslogAppenderBase.facilityStringToint("SYSLOG"));
    assertEquals(SyslogConstants.LOG_LPR, SyslogAppenderBase.facilityStringToint("LPR"));
    assertEquals(SyslogConstants.LOG_NEWS, SyslogAppenderBase.facilityStringToint("NEWS"));
    assertEquals(SyslogConstants.LOG_UUCP, SyslogAppenderBase.facilityStringToint("UUCP"));
    assertEquals(SyslogConstants.LOG_CRON, SyslogAppenderBase.facilityStringToint("CRON"));
    assertEquals(SyslogConstants.LOG_AUTHPRIV, SyslogAppenderBase.facilityStringToint("AUTHPRIV"));
    assertEquals(SyslogConstants.LOG_FTP, SyslogAppenderBase.facilityStringToint("FTP"));
    assertEquals(SyslogConstants.LOG_NTP, SyslogAppenderBase.facilityStringToint("NTP"));
    assertEquals(SyslogConstants.LOG_AUDIT, SyslogAppenderBase.facilityStringToint("AUDIT"));
    assertEquals(SyslogConstants.LOG_ALERT, SyslogAppenderBase.facilityStringToint("ALERT"));
    assertEquals(SyslogConstants.LOG_CLOCK, SyslogAppenderBase.facilityStringToint("CLOCK"));
    assertEquals(SyslogConstants.LOG_LOCAL0, SyslogAppenderBase.facilityStringToint("LOCAL0"));
    assertEquals(SyslogConstants.LOG_LOCAL1, SyslogAppenderBase.facilityStringToint("LOCAL1"));
    assertEquals(SyslogConstants.LOG_LOCAL2, SyslogAppenderBase.facilityStringToint("LOCAL2"));
    assertEquals(SyslogConstants.LOG_LOCAL3, SyslogAppenderBase.facilityStringToint("LOCAL3"));
    assertEquals(SyslogConstants.LOG_LOCAL4, SyslogAppenderBase.facilityStringToint("LOCAL4"));
    assertEquals(SyslogConstants.LOG_LOCAL5, SyslogAppenderBase.facilityStringToint("LOCAL5"));
    assertEquals(SyslogConstants.LOG_LOCAL6, SyslogAppenderBase.facilityStringToint("LOCAL6"));
    assertEquals(SyslogConstants.LOG_LOCAL7, SyslogAppenderBase.facilityStringToint("LOCAL7"));
  }

  @Test
  public void facilityStringIsCaseInsensitive() {
    assertEquals(SyslogConstants.LOG_LOCAL7, SyslogAppenderBase.facilityStringToint("local7"));
  }

  @Test
  public void rejectsUnknownFacilityString() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> SyslogAppenderBase.facilityStringToint("NOPE"));

    assertEquals("NOPE is not a valid syslog facility string", e.getMessage());
  }

  @Test
  public void trimsTheFacility() {
    appender.setFacility("  LOCAL0 ");

    assertEquals("LOCAL0", appender.getFacility());
  }

  @Test
  public void acceptsNullFacility() {
    appender.setFacility(null);

    assertNull(appender.getFacility());
  }

  @Test
  public void failsToStartWithoutFacility() {
    appender.setFacility(null);

    appender.start();

    assertFalse(appender.isStarted());
    assertStatus(Status.ERROR, "The Facility option is mandatory");
  }

  @Test
  public void startsWithFacilityAndConnection() {
    appender.start();

    assertTrue(appender.isStarted());
    assertEquals(1, appender.outputStreamsCreated);
    new StatusChecker(context).assertIsWarningOrErrorFree();
  }

  @Test
  public void failsToStartWhenSyslogHostIsUnknown() {
    UnknownHostException failure = new UnknownHostException("no.such.host");
    appender.unknownHost = failure;

    appender.start();

    assertFalse(appender.isStarted());
    assertSame(failure, assertStatus(Status.ERROR, "Could not create SyslogWriter").getThrowable());
  }

  @Test
  public void failsToStartWhenDatagramSocketCannotBeBound() {
    SocketException failure = new SocketException("no free port");
    appender.socketFailure = failure;

    appender.start();

    assertFalse(appender.isStarted());
    assertSame(failure, assertStatus(Status.WARN,
        "Failed to bind to a random datagram socket. Will try to reconnect later.").getThrowable());
  }

  @Test
  public void defaultsCharsetToThePlatformCharset() {
    appender.setCharset(null);

    appender.start();

    assertEquals(Charset.defaultCharset(), appender.getCharset());
  }

  @Test
  public void encodesMessagesWithTheConfiguredCharset() {
    Charset charset = Charset.forName("UTF-16BE");
    appender.setCharset(charset);
    appender.start();

    appender.append("héllo");

    assertSame(charset, appender.getCharset());
    assertEquals(1, appender.stream.messages.size());
    assertArrayEquals("héllo".getBytes(charset), appender.stream.messages.get(0));
  }

  @Test
  public void buildsLayoutOnStart() {
    appender.start();

    assertEquals(1, appender.layoutsBuilt);
    assertTrue(appender.getLayout() instanceof EchoLayout);
  }

  @Test
  public void keepsExistingLayoutOnStart() {
    Layout<String> layout = new EchoLayout();
    appender.layout = layout;

    appender.start();

    assertEquals(0, appender.layoutsBuilt);
    assertSame(layout, appender.getLayout());
  }

  @Test
  public void ignoresLayoutSetDirectly() {
    appender.setLayout(new EchoLayout());

    assertNull(appender.getLayout());
    assertStatus(Status.WARN, "The layout of a SyslogAppender cannot be set directly. See also "
        + SyslogAppenderBase.SYSLOG_LAYOUT_URL);
  }

  @Test
  public void defaultsMaxMessageSizeToSystemDatagramSize() {
    appender.sendBufferSize = 1000;

    appender.start();

    assertEquals(1000, appender.getMaxMessageSize());
    assertStatus(Status.INFO, "Defaulting maxMessageSize to [1000]");
  }

  @Test
  public void limitsDefaultMaxMessageSize() {
    appender.sendBufferSize = 100000;

    appender.start();

    assertEquals(SyslogAppenderBase.MAX_MESSAGE_SIZE_LIMIT, appender.getMaxMessageSize());
  }

  @Test
  public void warnsWhenMaxMessageSizeExceedsSystemDatagramSize() {
    appender.sendBufferSize = 1000;
    appender.setMaxMessageSize(2000);

    appender.start();

    assertTrue(appender.isStarted());
    assertEquals(2000, appender.getMaxMessageSize());
    assertStatus(Status.WARN,
        "maxMessageSize of [2000] is larger than the system defined datagram size of [1000].");
    assertStatus(Status.WARN, "This may result in dropped logs.");
  }

  @Test
  public void keepsMaxMessageSizeWithinSystemDatagramSize() {
    appender.sendBufferSize = 1000;
    appender.setMaxMessageSize(1000);

    appender.start();

    assertTrue(appender.isStarted());
    assertEquals(1000, appender.getMaxMessageSize());
    new StatusChecker(context).assertIsWarningOrErrorFree();
  }

  @Test
  public void writesAndFlushesEachEvent() {
    appender.start();

    appender.append("first");
    appender.append("second");

    assertEquals(2, appender.stream.messages.size());
    assertEquals("first", appender.stream.message(0));
    assertEquals("second", appender.stream.message(1));
  }

  @Test
  public void truncatesMessagesLongerThanMaxMessageSize() {
    appender.setMaxMessageSize(5);
    appender.start();

    appender.append("abcdefgh");
    appender.append("abcde");

    assertEquals("abcde", appender.stream.message(0));
    assertEquals("abcde", appender.stream.message(1));
  }

  @Test
  public void skipsEventsTheLayoutRendersAsNull() {
    appender.start();

    appender.append(null);

    assertEquals(0, appender.stream.messages.size());
    assertEquals(0, appender.stream.pending.size());
  }

  @Test
  public void reportsErrorWhenSendingFails() {
    appender.start();
    IOException failure = new IOException("send failed");
    appender.stream.flushFailure = failure;

    appender.append("event");

    assertSame(failure, assertStatus(Status.ERROR, "Failed to send diagram to localhost").getThrowable());
  }

  @Test
  public void ignoresEventsWhenStopped() {
    appender.start();
    appender.stop();

    appender.append("event");

    assertTrue(appender.stream.closed);
    assertEquals(0, appender.stream.messages.size());
    assertEquals(0, appender.stream.pending.size());
  }

  @Test
  public void lazyAppenderConnectsOnFirstEvent() {
    appender.setLazy(true);

    appender.start();

    assertTrue(appender.isStarted());
    assertEquals(0, appender.outputStreamsCreated);

    appender.append("first");
    appender.append("second");

    assertEquals(1, appender.outputStreamsCreated);
    assertEquals("first", appender.stream.message(0));
    assertEquals("second", appender.stream.message(1));
  }

  @Test
  public void lazyAppenderDropsEventsWhenItCannotConnect() {
    appender.setLazy(true);
    appender.socketFailure = new SocketException("no free port");
    appender.start();

    appender.append("first");
    appender.append("second");

    assertTrue(appender.isStarted());
    assertNull(appender.stream);
    assertEquals(1, appender.outputStreamsCreated);
    assertStatus(Status.WARN, "Failed to bind to a random datagram socket. Will try to reconnect later.");
  }

  @Test
  public void hasDefaultProperties() {
    TestSyslogAppender unconfigured = new TestSyslogAppender();

    assertEquals(SyslogConstants.SYSLOG_PORT, unconfigured.getPort());
    assertFalse(unconfigured.getLazy());
    assertEquals(0, unconfigured.getMaxMessageSize());
    assertNull(unconfigured.getSyslogHost());
    assertNull(unconfigured.getSuffixPattern());
    assertNull(unconfigured.getCharset());
    assertNull(unconfigured.getLayout());
  }

  @Test
  public void returnsConfiguredProperties() {
    Charset charset = Charset.forName("US-ASCII");

    appender.setSyslogHost("syslog.example.com");
    appender.setPort(1514);
    appender.setLazy(true);
    appender.setMaxMessageSize(1234);
    appender.setSuffixPattern("%msg");
    appender.setCharset(charset);

    assertEquals("syslog.example.com", appender.getSyslogHost());
    assertEquals(1514, appender.getPort());
    assertTrue(appender.getLazy());
    assertEquals(1234, appender.getMaxMessageSize());
    assertEquals("%msg", appender.getSuffixPattern());
    assertSame(charset, appender.getCharset());
  }

  private Status assertStatus(int level, String message) {
    for (Status status : context.getStatusManager().getCopyOfStatusList()) {
      if (status.getLevel() == level && message.equals(status.getMessage())) {
        return status;
      }
    }
    fail("no status of level " + level + " with message: " + message);
    return null;
  }

  /**
   * A syslog appender whose output stream records what it is sent (or cannot
   * be created), with an {@link EchoLayout} as its default layout.
   */
  private static class TestSyslogAppender extends SyslogAppenderBase<String> {

    UnknownHostException unknownHost;
    SocketException socketFailure;
    int sendBufferSize = 1000;
    int outputStreamsCreated;
    int layoutsBuilt;
    RecordingSyslogOutputStream stream;

    @Override
    public SyslogOutputStream createOutputStream() throws UnknownHostException, SocketException {
      outputStreamsCreated++;
      if (unknownHost != null) {
        throw unknownHost;
      }
      if (socketFailure != null) {
        throw socketFailure;
      }
      stream = new RecordingSyslogOutputStream(sendBufferSize);
      return stream;
    }

    @Override
    public Layout<String> buildLayout() {
      layoutsBuilt++;
      return new EchoLayout();
    }

    @Override
    public int getSeverityForEvent(Object eventObject) {
      return SyslogConstants.INFO_SEVERITY;
    }
  }

  /** Renders an event as itself. */
  private static class EchoLayout extends LayoutBase<String> {

    @Override
    public String doLayout(String event) {
      return event;
    }
  }

  /**
   * Records each flushed message instead of sending it, and reports the given
   * size as the system's datagram size.
   */
  private static class RecordingSyslogOutputStream extends SyslogOutputStream {

    final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    final List<byte[]> messages = new ArrayList<byte[]>();
    final int sendBufferSize;
    IOException flushFailure;
    boolean closed;

    RecordingSyslogOutputStream(int sendBufferSize) throws UnknownHostException, SocketException {
      super("127.0.0.1", SyslogConstants.SYSLOG_PORT);
      this.sendBufferSize = sendBufferSize;
    }

    @Override
    public void write(byte[] byteArray, int offset, int len) {
      pending.write(byteArray, offset, len);
    }

    @Override
    public void flush() throws IOException {
      if (flushFailure != null) {
        throw flushFailure;
      }
      messages.add(pending.toByteArray());
      pending.reset();
    }

    @Override
    int getSendBufferSize() {
      return sendBufferSize;
    }

    @Override
    public void close() {
      closed = true;
      super.close();
    }

    String message(int index) {
      return new String(messages.get(index), UTF_8);
    }
  }
}
