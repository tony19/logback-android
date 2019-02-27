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
package ch.qos.logback.core.net;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link ch.qos.logback.core.net.AutoFlushingObjectWriter}.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class AutoFlushingObjectWriterTest {

  private InstrumentedObjectOutputStream objectOutputStream;

  @Before
  public void beforeEachTest() throws IOException {
    objectOutputStream = spy(new InstrumentedObjectOutputStream());
  }

  @Test
  public void writesToUnderlyingObjectOutputStream() throws IOException {

    // given
    ObjectWriter objectWriter = new AutoFlushingObjectWriter(objectOutputStream, 2);
    String object = "foo";

    // when
    objectWriter.write(object);

    // then
    verify(objectOutputStream).writeObjectOverride(object);
  }

  @Test
  public void flushesAfterWrite() throws IOException {

    // given
    ObjectWriter objectWriter = new AutoFlushingObjectWriter(objectOutputStream, 2);
    String object = "foo";

    // when
    objectWriter.write(object);

    // then
    InOrder inOrder = inOrder(objectOutputStream);
    inOrder.verify(objectOutputStream).writeObjectOverride(object);
    inOrder.verify(objectOutputStream).flush();
  }

  @Test
  public void resetsObjectOutputStreamAccordingToGivenResetFrequency() throws IOException {

    // given
    ObjectWriter objectWriter = new AutoFlushingObjectWriter(objectOutputStream, 2);
    String object = "foo";

    // when
    objectWriter.write(object);
    objectWriter.write(object);
    objectWriter.write(object);
    objectWriter.write(object);

    // then
    InOrder inOrder = inOrder(objectOutputStream);
    inOrder.verify(objectOutputStream).writeObjectOverride(object);
    inOrder.verify(objectOutputStream).writeObjectOverride(object);
    inOrder.verify(objectOutputStream).reset();
    inOrder.verify(objectOutputStream).writeObjectOverride(object);
    inOrder.verify(objectOutputStream).writeObjectOverride(object);
    inOrder.verify(objectOutputStream).reset();
  }

}
