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
package ch.qos.logback.classic.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * Provides inputs and outputs for objects to and from files.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class ObjectIOProvider {

  /**
   * Creates a new {@link ObjectOutput} for the given {@code fileName}.
   *
   * @param fileName the name of the file for which the output should be created
   * @return a new instance of {@link ObjectOutput}
   * @throws IOException when an exception occurred during the creation of the stream
   */
  public ObjectOutput newObjectOutput(final String fileName) throws IOException {
    return new ObjectOutputStream(new FileOutputStream(fileName));
  }

  /**
   * Creates a new {@link ObjectInput} for the given {@code file}.
   *
   * @param file the file for which the input should be cerated
   * @return a new instance of {@link ObjectInput}
   * @throws IOException when an exception occurred during the creation of the stream
   */
  public ObjectInput newObjectInput(final File file) throws IOException {
    return new ObjectInputStream(new FileInputStream(file));
  }
}
