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
package ch.qos.logback.core;

import org.junit.Test;

public class NOPOutputStreamTest {

  @Test
  public void discardsEveryWriteWithoutFailing() throws Exception {
    NOPOutputStream out = new NOPOutputStream();

    out.write(0);
    out.write(255);
    out.write(-1);
    out.write(new byte[] {1, 2, 3});
    out.write(new byte[] {1, 2, 3}, 1, 2);
    out.flush();
    out.close();
    // still usable after close: it holds no resource
    out.write('x');
  }
}
