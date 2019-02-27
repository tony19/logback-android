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
package ch.qos.logback.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This stream writes its output to the target PrintStream supplied to its
 * constructor. At the same time, all the available bytes are collected and
 * returned by the toString() method.
 * 
 * @author Ceki Gulcu
 */
public class TeeOutputStream extends OutputStream {

  final PrintStream targetPS;
  public final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  public TeeOutputStream(PrintStream targetPS) {
    // allow for null arguments
    this.targetPS = targetPS;
  }

  public void write(int b) throws IOException {
    baos.write(b);
    if(targetPS != null) {
      targetPS.write(b);
    }
  }

  public String toString() {
    return baos.toString();
  }

  public byte[] toByteArray() {
    return baos.toByteArray();
  }
}
