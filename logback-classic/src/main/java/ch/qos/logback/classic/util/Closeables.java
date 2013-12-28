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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * Provides convenience methods to close I/O objects.
 *
 * @author Sebastian Gr&ouml;bler
 */
public final class Closeables {

  private Closeables() {
    // no public instantiation allowed
  }

  /**
   * Gracefully closes given {@code objectInput}, disregarding
   * if the given object is {@code null} or its close method throws
   * an {@link IOException}.
   *
   * @param objectInput the {@link ObjectOutput} to close
   */
  public static void close(final ObjectInput objectInput) {
    if (objectInput == null) {
      return;
    }

    try {
      objectInput.close();
    } catch (final IOException e) {
      // ignored
    }
  }

  /**
   * Gracefully closes given {@code objectOutput}, disregarding
   * if the given object is {@code null} or its close method throws
   * an {@link IOException}.
   *
   * @param objectOutput the {@link ObjectOutput} to close
   */
  public static void close(final ObjectOutput objectOutput) {
    if (objectOutput == null) {
      return;
    }

    try {
      objectOutput.close();
    } catch (final IOException e) {
      // ignored
    }
  }
}
