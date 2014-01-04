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
package ch.qos.logback.classic.util;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Sebastian Gr&ouml;bler
 */
public class CloseablesTest {

  @Test
  public void closeOfObjectInputIsGracefulToNullValue() {
    Closeables.close((ObjectInput) null);
  }

  @Test
  public void closeOfObjectInputIsGracefulToIOException() throws IOException {
    final ObjectInput objectInput = mock(ObjectInput.class);
    doThrow(IOException.class).when(objectInput).close();

    Closeables.close(objectInput);
  }

  @Test
  public void closeOfObjectOutputIsGracefulToNullValue() {
    Closeables.close((ObjectOutput) null);
  }

  @Test
  public void closeOfObjectOutputIsGracefulToIOException() throws IOException {
    final ObjectOutput objectOutput = mock(ObjectOutput.class);
    doThrow(IOException.class).when(objectOutput).close();

    Closeables.close(objectOutput);
  }
}
