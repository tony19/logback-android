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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;

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

  @Before
  public void setUp() throws Exception {
    client.setContext(context);
    client.setQueue(queue);
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

}
