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
package ch.qos.logback.classic.net.testObjectBuilders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.classic.util.Closeables;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import org.joda.time.DateTime;

/**
 * Allows to easily create serialized log events by utilizing the {@link MockLoggingEvent}.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class SerializedLogFileFactory {

  /**
   * Creates a file which is a serialized version of the {@link ch.qos.logback.classic.net.testObjectBuilders.MockLoggingEvent#create()}
   * result at the given {@code filePath} and with the specified {@code lastModified}.
   *
   * @param filePath the path under which de logging event should be saved
   * @param lastModified the date and time of the last modification of the file
   * @throws IOException when an exception occurred during the creation of the file
   */
  public static void addFile(final String filePath, final DateTime lastModified) throws IOException {
    addFile(MockLoggingEvent.create(), filePath, lastModified);
  }

  /**
   * Serializes the given {@code loggingEvent} to a file at the specified {@code filePath} with the specified
   * {@code lastModified}.
   *
   * @param loggingEvent the implementation of {@link ILoggingEvent} to serialize to file
   * @param filePath the path to serialize the logging event to
   * @param lastModified the date and time of the last modification of the file
   * @throws IOException when an exception occurred during the creation of the file
   */
  public static void addFile(
          final ILoggingEvent loggingEvent,
          final String filePath,
          final DateTime lastModified) throws IOException {

    ObjectOutput objectOutput = null;
    try {
      objectOutput = new ObjectOutputStream(new FileOutputStream(filePath));
      objectOutput.writeObject(LoggingEventVO.build(loggingEvent));
    } finally {
      Closeables.close(objectOutput);
    }

    final File file = new File(filePath);
    file.setLastModified(lastModified.getMillis());
  }
}
