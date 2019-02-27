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
package ch.qos.logback.core.joran.spi;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The set of console output targets.

 * @author Ruediger Dohna
 * @author Ceki G&uuml;lc&uuml;
 * @author Tom SH Liu
 * @author David Roussel
 *
 * @deprecated This will be removed along with ConsoleAppender in an upcoming release.
 */
@Deprecated
public enum ConsoleTarget {

  SystemOut("System.out", new OutputStream() {
    @Override
    public void write(int b) throws IOException {
      System.out.write(b);
    }
    @Override
    public void write(byte b[]) throws IOException {
      System.out.write(b);
    }
    @Override
    public void write(byte b[], int off, int len) throws IOException {
      System.out.write(b, off, len);
    }
    @Override
    public void flush() throws IOException {
      System.out.flush();
    }
  }),

  SystemErr("System.err", new OutputStream() {
    @Override
    public void write(int b) throws IOException {
      System.err.write(b);
    }
    @Override
    public void write(byte b[]) throws IOException {
      System.err.write(b);
    }
    @Override
    public void write(byte b[], int off, int len) throws IOException {
      System.err.write(b, off, len);
    }
    @Override
    public void flush() throws IOException {
      System.err.flush();
    }
  });

  public static ConsoleTarget findByName(String name) {
    for (ConsoleTarget target : ConsoleTarget.values()) {
      if (target.name.equalsIgnoreCase(name)) {
        return target;
      }
    }
    return null;
  }

  private final String name;
  private final OutputStream stream;

  private ConsoleTarget(String name, OutputStream stream) {
    this.name = name;
    this.stream = stream;
  }

  public String getName() {
    return name;
  }

  public OutputStream getStream() {
    return stream;
  }

  @Override
  public String toString() {
    return name;
  }
}
