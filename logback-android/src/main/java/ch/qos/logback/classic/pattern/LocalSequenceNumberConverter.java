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
package ch.qos.logback.classic.pattern;

import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A converters based on a a locally incremented sequence number. The sequence number is
 * initialized to the number of milliseconds elapsed since 1970-01-01 until this instance
 * is initialized.
 *
 * <p>
 * <b>EXPERIMENTAL</b> This class is experimental and may be removed in the future.
 *
 */
public class LocalSequenceNumberConverter extends ClassicConverter {

  AtomicLong sequenceNumber = new AtomicLong(System.currentTimeMillis());

  @Override
  public String convert(ILoggingEvent event) {
    return Long.toString(sequenceNumber.getAndIncrement());
  }
}
