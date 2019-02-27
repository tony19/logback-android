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
package ch.qos.logback.core.appender;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.encoder.DummyEncoder;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.layout.DummyLayout;


public class DummyAppenderTest extends AbstractAppenderTest<Object> {

  ByteArrayOutputStream baos = new ByteArrayOutputStream();
  DummyWriterAppender<Object> da = new DummyWriterAppender<Object>(baos);
   
  protected Appender<Object> getAppender() {
    return da;
  }
  
  protected Appender<Object> getConfiguredAppender() {
    da.setEncoder(new DummyEncoder<Object>());
    da.start();
    return da;
  }

  @Test
  public void testBasic() throws IOException {
    Encoder<Object> encoder = new DummyEncoder<Object>();
    da.setEncoder(encoder);
    da.start();
    da.doAppend(new Object());
    assertEquals(DummyLayout.DUMMY, baos.toString());
  }
  
}
