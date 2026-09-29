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
package ch.qos.logback.core.net.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Unit tests for {@link RemoteReceiverStreamClient}.
 *
 * @author Carl Harris
 */
public class RemoteReceiverStreamClientTest {

  private static final String TEST_EVENT = "test event";

  private MockContext context = new MockContext();

  private MockEventQueue queue = new MockEventQueue();

  private ByteArrayOutputStream outputStream =
      new ByteArrayOutputStream();

  private RemoteReceiverStreamClient client =
      new RemoteReceiverStreamClient("someId", outputStream);

  private final ContextBase statusContext = new ContextBase();
  private final StatusChecker checker = new StatusChecker(statusContext);

  @Before
  public void setUp() throws Exception {
    client.setContext(context);
    client.setQueue(queue);
  }

  @After
  public void tearDown() {
    // run() on the test thread ends by restoring the interrupt it got from
    // the drained MockEventQueue; don't leak it into other tests
    Thread.interrupted();
  }

  @Test
  public void testOfferEventAndRun() throws Exception {
    client.offer(TEST_EVENT);

    Thread thread = new Thread(client);
    thread.start();

    // MockEventQueue will interrupt the thread when the queue is drained
    thread.join(1000);
    assertFalse(thread.isAlive());

    ObjectInputStream ois = new ObjectInputStream(
        new ByteArrayInputStream(outputStream.toByteArray()));
    assertEquals(TEST_EVENT, ois.readObject());
  }

  @Test
  public void testOfferEventSequenceAndRun() throws Exception {
    for (int i = 0; i < 10; i++) {
      client.offer(TEST_EVENT + i);
    }

    Thread thread = new Thread(client);
    thread.start();
    thread.join(1000);
    assertFalse(thread.isAlive());

    ObjectInputStream ois = new ObjectInputStream(
        new ByteArrayInputStream(outputStream.toByteArray()));
    for (int i = 0; i < 10; i++) {
      assertEquals(TEST_EVENT + i, ois.readObject());
    }
  }

  @Test
  public void offerWithoutAQueueIsRejected() {
    final RemoteReceiverStreamClient unqueued =
        new RemoteReceiverStreamClient("someId", outputStream);

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> unqueued.offer(TEST_EVENT));

    assertEquals("client has no event queue", ex.getMessage());
  }

  @Test
  public void streamIsResetAfterEveryOosResetFrequencyEvents()
      throws Exception {
    final int frequency = CoreConstants.OOS_RESET_FREQUENCY;
    // the same instance each time, so the stream writes back-references to
    // it until the stream is reset
    for (int i = 0; i < frequency + 2; i++) {
      assertTrue(client.offer(TEST_EVENT));
    }

    client.run();
    assertTrue(Thread.interrupted());

    ObjectInputStream ois = new ObjectInputStream(
        new ByteArrayInputStream(outputStream.toByteArray()));
    Object[] events = new Object[frequency + 2];
    for (int i = 0; i < events.length; i++) {
      events[i] = ois.readObject();
      assertEquals(TEST_EVENT, events[i]);
    }
    assertSame(events[0], events[frequency - 1]);
    // the reset after the frequency-th event makes it write the event anew
    assertNotSame(events[frequency - 1], events[frequency]);
    // and then counting starts over, so the next one is a back-reference
    assertSame(events[frequency], events[frequency + 1]);
  }

  @Test
  public void socketExceptionClosesTheClientWithAnInfoStatus() {
    RemoteReceiverStreamClient failing = new RemoteReceiverStreamClient(
        "someId", new FailingOutputStream(
            new SocketException("Connection reset")));
    failing.setContext(statusContext);
    failing.setQueue(queue);

    failing.run();

    checker.assertContainsMatch(Status.INFO,
        "client someId: java.net.SocketException: Connection reset");
    checker.assertContainsMatch(Status.INFO,
        "client someId: connection closed");
    checker.assertIsErrorFree();
  }

  @Test
  public void ioExceptionClosesTheClientAndStreamWithAnErrorStatus() {
    CloseRecordingOutputStream stream = new CloseRecordingOutputStream();
    RemoteReceiverStreamClient failing =
        new RemoteReceiverStreamClient("someId", stream);
    failing.setContext(statusContext);
    failing.setQueue(queue);
    failing.offer(new UnserializableEvent());

    failing.run();

    checker.assertContainsMatch(Status.ERROR,
        "client someId: java.io.NotSerializableException: java.lang.Object");
    checker.assertContainsMatch(Status.INFO,
        "client someId: connection closed");
    assertTrue(stream.closed);
  }

  @Test
  public void runtimeExceptionClosesTheClientWithAnErrorStatus() {
    RemoteReceiverStreamClient failing =
        new RemoteReceiverStreamClient("someId", outputStream);
    failing.setContext(statusContext);
    failing.setQueue(new LinkedBlockingQueue<Serializable>() {
      private static final long serialVersionUID = 1L;

      @Override
      public Serializable take() {
        throw new IllegalStateException("queue broken");
      }
    });

    failing.run();

    checker.assertContainsMatch(Status.ERROR,
        "client someId: java.lang.IllegalStateException: queue broken");
    checker.assertContainsMatch(Status.INFO,
        "client someId: connection closed");
  }

  /** An output stream whose every write fails with the given exception. */
  private static class FailingOutputStream extends OutputStream {

    private final IOException failure;

    FailingOutputStream(IOException failure) {
      this.failure = failure;
    }

    @Override
    public void write(int b) throws IOException {
      throw failure;
    }
  }

  /** An output stream that records whether it was closed. */
  private static class CloseRecordingOutputStream
      extends ByteArrayOutputStream {

    boolean closed;

    @Override
    public void close() {
      closed = true;
    }
  }

  /** An event with a field that cannot be serialized. */
  private static class UnserializableEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final Object payload = new Object();
  }

}
