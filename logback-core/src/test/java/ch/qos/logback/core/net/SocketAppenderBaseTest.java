/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.net;

import ch.qos.logback.core.spi.PreSerializationTransformer;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sebastian Gr&ouml;bler
 */
public class SocketAppenderBaseTest {

  private final SocketAppenderBase socketAppenderBase = mock(SocketAppenderBase.class);

  @Before
  public void beforeEachTest() throws IOException {
    // setup default state and behaviour
    socketAppenderBase.setRemoteHost("localhost");
    socketAppenderBase.oos = MockObjectOutputStream.createNopMock();
    when(socketAppenderBase.getPST()).thenReturn(mock(PreSerializationTransformer.class));

    // make sure real methods are called on the abstract class
    doCallRealMethod().when(socketAppenderBase).tryAppend(anyObject());
    doCallRealMethod().when(socketAppenderBase).fireConnector();
    doCallRealMethod().when(socketAppenderBase).isConnecting();
  }

  @Test
  public void isConnectingReturnsTrueWhenThereIsAConnectorThreadRunning() {
    socketAppenderBase.fireConnector();
    assertTrue(socketAppenderBase.isConnecting());
  }

  @Test
  public void isConnectingReturnsFalseWhenThereIsNoConnectorThreadRunning() {
    assertFalse(socketAppenderBase.isConnecting());
  }

  @Test
  public void tryAppendReturnsTrueOnSuccessfulAppend() {
    assertTrue(socketAppenderBase.tryAppend(mock(Object.class)));
  }

  @Test
  public void tryAppendReturnsFalseOnNullEvent() {
    assertFalse(socketAppenderBase.tryAppend(null));
  }

  @Test
  public void tryAppendReturnsFalseOnMissingAddress() {
    socketAppenderBase.address = null;
    assertFalse(socketAppenderBase.tryAppend(mock(Object.class)));
  }

  @Test
  public void tryAppendReturnsFalseWhenThereIsNoConnection() {
    socketAppenderBase.oos = null;
    assertFalse(socketAppenderBase.tryAppend(mock(Object.class)));
  }

  @Test
  public void tryAppendReturnsFalseWhenThereIsAnIOException() {
    socketAppenderBase.oos = MockObjectOutputStream.createIOExceptionThrowingMock();
    assertFalse(socketAppenderBase.tryAppend(mock(Object.class)));
  }

  /**
   * Allows mocking of {@link ObjectOutputStream#writeObject(Object)} by
   * redirecting to {@link ObjectOutputStream#writeObjectOverride(Object)}.
   */
  public static class MockObjectOutputStream extends ObjectOutputStream {

    public static ObjectOutputStream createNopMock() {
      return createMock(false);
    }

    public static ObjectOutputStream createIOExceptionThrowingMock() {
      return createMock(true);
    }

    private static ObjectOutputStream createMock(final boolean throwIOException) {
      try {
        return new MockObjectOutputStream(throwIOException);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }

    private final boolean throwIOException;

    protected MockObjectOutputStream(boolean throwIOException) throws IOException {
      super();
      this.throwIOException = throwIOException;
    }

    @Override
    protected void writeObjectOverride(final Object obj) throws IOException {
      if (throwIOException) {
        throw new IOException();
      }
    }

    @Override
    public void flush() throws IOException {
      // NOP
    }

    @Override
    public void close() throws IOException {
      // NOP
    }
  }
}
