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
package ch.qos.logback.classic.net.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.SequenceInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.net.mock.MockAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.read.ListAppender;

/**
 * Unit tests for {@link RemoteAppenderStreamClient}.
 *
 * @author Carl Harris
 */
public class RemoteAppenderStreamClientTest {

  private static final String CLIENT = "client some client ID";

  private final LoggerContext lc = new LoggerContext();
  private final ListAppender<ILoggingEvent> clientLog = new ListAppender<ILoggingEvent>();
  private MockAppender appender;
  private Logger logger;
  private LoggingEvent event;
  private RemoteAppenderStreamClient client;

  @Before
  public void setUp() throws Exception {
    appender = new MockAppender();
    appender.start();

    logger = lc.getLogger(getClass());
    logger.addAppender(appender);

    // what the client itself logs, as opposed to the events it receives
    clientLog.setContext(lc);
    clientLog.start();
    Logger clientLogger = lc.getLogger(RemoteAppenderStreamClient.class.getPackage().getName());
    clientLogger.addAppender(clientLog);
    logger.setAdditive(false);

    event = new LoggingEvent(logger.getName(), logger,
        Level.DEBUG, "test message", null, new Object[0]);

    LoggingEventVO eventVO = LoggingEventVO.build(event);

    client = newClient(new ByteArrayInputStream(serialize(eventVO)));
  }

  @After
  public void tearDown() {
    lc.stop();
  }

  @Test
  public void testWithEnabledLevel() throws Exception {
    logger.setLevel(Level.DEBUG);
    client.run();
    client.close();

    ILoggingEvent rcvdEvent = appender.getLastEvent();
    assertEquals(event.getLoggerName(), rcvdEvent.getLoggerName());
    assertEquals(event.getLevel(), rcvdEvent.getLevel());
    assertEquals(event.getMessage(), rcvdEvent.getMessage());
  }

  @Test
  public void testWithDisabledLevel() throws Exception {
    logger.setLevel(Level.INFO);
    client.run();
    client.close();
    assertNull(appender.getLastEvent());
  }

  @Test
  public void readFailureIsLoggedAndEndsTheConnection() throws Exception {
    final IOException failure = new IOException("connection reset");
    InputStream failing = new SequenceInputStream(new ByteArrayInputStream(serialize()),
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw failure;
          }
        });

    newClient(failing).run();

    assertClientLogged(Level.INFO, CLIENT + ": " + failure);
    assertClientLogged(Level.INFO, CLIENT + ": connection closed");
  }

  @Test
  public void unknownEventClassIsLoggedAndEndsTheConnection() throws Exception {
    newClient(new ByteArrayInputStream(serializedObjectOfMissingClass())).run();

    assertClientLogged(Level.ERROR, CLIENT + ": unknown event class");
    assertClientLogged(Level.INFO, CLIENT + ": connection closed");
  }

  @Test
  public void objectThatIsNotAnEventIsLoggedAndEndsTheConnection() throws Exception {
    newClient(new ByteArrayInputStream(serialize("not an event"))).run();

    ILoggingEvent error = assertClientLogged(Level.ERROR, CLIENT + ": java.lang.ClassCastException");
    assertTrue(error.getFormattedMessage().startsWith(CLIENT + ": java.lang.ClassCastException"));
    assertNull(appender.getLastEvent());
    assertClientLogged(Level.INFO, CLIENT + ": connection closed");
  }

  @Test
  public void streamWithoutHeaderEndsTheConnectionQuietly() throws Exception {
    newClient(new ByteArrayInputStream(new byte[0])).run();

    assertClientLogged(Level.INFO, CLIENT + ": connected");
    assertClientLogged(Level.INFO, CLIENT + ": connection closed");
    assertEquals(2, clientLog.list.size());
  }

  private RemoteAppenderStreamClient newClient(InputStream in) {
    RemoteAppenderStreamClient newClient = new RemoteAppenderStreamClient("some client ID", in);
    newClient.setLoggerContext(lc);
    return newClient;
  }

  /**
   * Asserts that the client logged a message at the given level that starts
   * with the given prefix.
   */
  private ILoggingEvent assertClientLogged(Level level, String prefix) {
    for (ILoggingEvent logged : clientLog.list) {
      if (logged.getLevel() == level && logged.getFormattedMessage().startsWith(prefix)) {
        return logged;
      }
    }
    throw new AssertionError("no " + level + " [" + prefix + "...] in " + clientLog.list);
  }

  private static byte[] serialize(Object... objects) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    for (Object object : objects) {
      oos.writeObject(object);
    }
    oos.close();
    return bos.toByteArray();
  }

  /**
   * A serialization stream holding one object of a proxy class that
   * implements a missing interface: reading it fails with
   * {@link ClassNotFoundException}.
   */
  private static byte[] serializedObjectOfMissingClass() throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(bytes);
    out.writeShort(ObjectStreamConstants.STREAM_MAGIC);
    out.writeShort(ObjectStreamConstants.STREAM_VERSION);
    out.writeByte(ObjectStreamConstants.TC_OBJECT);
    out.writeByte(ObjectStreamConstants.TC_PROXYCLASSDESC);
    out.writeInt(1);
    out.writeUTF("does.not.Exist");
    out.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA);
    out.writeByte(ObjectStreamConstants.TC_NULL);
    out.close();
    return bytes.toByteArray();
  }
}
