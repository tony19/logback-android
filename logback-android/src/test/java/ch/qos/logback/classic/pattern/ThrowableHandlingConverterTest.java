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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class ThrowableHandlingConverterTest {

  @Test
  public void customThrowableHandlingConverterHandlesThrowables() {
    ThrowableHandlingConverter converter = new ThrowableHandlingConverter() {
      @Override
      public String convert(ILoggingEvent event) {
        return "";
      }
    };
    assertTrue(converter.handlesThrowable());
  }

  @Test
  public void builtInThrowableConvertersHandleThrowables() {
    assertTrue(new ThrowableProxyConverter().handlesThrowable());
    assertTrue(new ExtendedThrowableProxyConverter().handlesThrowable());
    assertTrue(new RootCauseFirstThrowableProxyConverter().handlesThrowable());
    assertTrue(new NopThrowableInformationConverter().handlesThrowable());
  }
}
