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
package ch.qos.logback.classic.corpus;

import java.io.IOException;
import java.util.Random;

public class ExceptionBuilder {

  static Throwable build(Random r, double nestingProbability) {
    double rn = r.nextDouble();
    boolean nested = false;
    if (rn < nestingProbability) {
      nested = true;
    }

    Throwable cause = null;
    if(nested) {
      cause = makeThrowable(r, null);
    } 
    return makeThrowable(r, cause);
  }

  private static Throwable makeThrowable(Random r, Throwable cause) {
    int exType = r.nextInt(4);
    switch(exType) {
    case 0: return new IllegalArgumentException("an illegal argument was passed", cause);
    case 1: return new Exception("this is a test", cause);
    // exType 2 is JMXProviderException (which is an IOException).
    // JMX not supported in Android, so use IOException.
    case 2: return new IOException("jmx provider exception error occured", cause);
    case 3: return new OutOfMemoryError("ran out of memory");
    }
    return null;
  }
  
}
