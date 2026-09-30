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
package ch.qos.logback.core.encoder;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EncoderBaseTest {

  // NopEncoder inherits the lifecycle of EncoderBase unchanged
  private final EncoderBase<Object> encoder = new NopEncoder<Object>();

  @Test
  public void isNotStartedInitially() {
    assertFalse(encoder.isStarted());
  }

  @Test
  public void startMarksEncoderStarted() {
    encoder.start();

    assertTrue(encoder.isStarted());
  }

  @Test
  public void stopMarksEncoderStopped() {
    encoder.start();

    encoder.stop();

    assertFalse(encoder.isStarted());
  }
}
