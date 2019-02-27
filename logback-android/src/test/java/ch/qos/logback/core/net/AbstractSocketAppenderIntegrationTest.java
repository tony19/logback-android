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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.net.server.ServerSocketUtil;
import ch.qos.logback.core.spi.PreSerializationTransformer;
import ch.qos.logback.core.util.ExecutorServiceUtil;

/**
 * Integration tests for {@link ch.qos.logback.core.net.AbstractSocketAppender}.
 *
 * @author Carl Harris
 * @author Sebastian Gr&ouml;bler
 */
public class AbstractSocketAppenderIntegrationTest {

    private static final int TIMEOUT = 2000;

    private ScheduledExecutorService executorService = ExecutorServiceUtil.newScheduledExecutorService();
    private MockContext mockContext = new MockContext(executorService);
    private AutoFlushingObjectWriter objectWriter;
    private ObjectWriterFactory objectWriterFactory = new SpyProducingObjectWriterFactory();
    private LinkedBlockingDeque<String> deque = spy(new LinkedBlockingDeque<String>(1));
    private QueueFactory queueFactory = mock(QueueFactory.class);
    private InstrumentedSocketAppender instrumentedAppender = new InstrumentedSocketAppender(queueFactory, objectWriterFactory);

    @Before
    public void setUp() throws Exception {
        when(queueFactory.<String> newLinkedBlockingDeque(anyInt())).thenReturn(deque);
        instrumentedAppender.setContext(mockContext);
    }

    @After
    public void tearDown() throws Exception {
        instrumentedAppender.stop();
        assertFalse(instrumentedAppender.isStarted());
        executorService.shutdownNow();
        assertTrue(executorService.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void dispatchesEvents() throws Exception {

        // given
        ServerSocket serverSocket = ServerSocketUtil.createServerSocket();
        instrumentedAppender.setRemoteHost(serverSocket.getInetAddress().getHostAddress());
        instrumentedAppender.setPort(serverSocket.getLocalPort());
        instrumentedAppender.start();

        Socket appenderSocket = serverSocket.accept();
        serverSocket.close();

        // when
        instrumentedAppender.append("some event");

        // wait for event to be taken from deque and being written into the stream
        verify(deque, timeout(TIMEOUT).atLeastOnce()).takeFirst();
        verify(objectWriter, timeout(TIMEOUT)).write("some event");

        // then
        ObjectInputStream ois = new ObjectInputStream(appenderSocket.getInputStream());
        assertEquals("some event", ois.readObject());
        appenderSocket.close();
    }

    private static class InstrumentedSocketAppender extends AbstractSocketAppender<String> {

        public InstrumentedSocketAppender(QueueFactory queueFactory, ObjectWriterFactory objectWriterFactory) {
            super(queueFactory, objectWriterFactory);
        }

        @Override
        protected void postProcessEvent(String event) {
        }

        @Override
        protected PreSerializationTransformer<String> getPST() {
            return new PreSerializationTransformer<String>() {
                public Serializable transform(String event) {
                    return event;
                }
            };
        }
    }

    private class SpyProducingObjectWriterFactory extends ObjectWriterFactory {

        @Override
        public AutoFlushingObjectWriter newAutoFlushingObjectWriter(OutputStream outputStream) throws IOException {
            objectWriter = spy(super.newAutoFlushingObjectWriter(outputStream));
            return objectWriter;
        }
    }
}
