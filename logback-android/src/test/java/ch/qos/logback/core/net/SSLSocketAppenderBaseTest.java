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

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.net.mock.MockContext;
import ch.qos.logback.core.spi.PreSerializationTransformer;

/**
 * Unit tests for {@link SSLSocketAppenderBase}.
 *
 * @author Carl Harris
 */
public class SSLSocketAppenderBaseTest {

  private MockContext context = new MockContext();

  private InstrumentedSSLSocketAppenderBase appender =
      new InstrumentedSSLSocketAppenderBase();

  @Before
  public void setUp() throws Exception {
    appender.setContext(context);
  }

  @Test
  public void testUsingDefaultConfig() throws Exception {
    // should be able to start successfully with no SSL configuration at all
    appender.start();
    assertNotNull(appender.getSocketFactory());
  }

  private static class InstrumentedSSLSocketAppenderBase
      extends SSLSocketAppenderBase<Object> {

    @Override
    protected void postProcessEvent(Object event) {
      throw new UnsupportedOperationException();
    }

    @Override
    protected PreSerializationTransformer<Object> getPST() {
      throw new UnsupportedOperationException();
    }

  }
}
