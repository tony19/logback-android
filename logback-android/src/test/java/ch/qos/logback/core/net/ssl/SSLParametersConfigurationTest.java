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
package ch.qos.logback.core.net.ssl;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.net.ssl.mock.MockSSLConfigurable;


/**
 * Unit tests for {@link SSLParametersConfiguration}.
 *
 * @author Carl Harris
 */
public class SSLParametersConfigurationTest {

  private MockSSLConfigurable configurable = new MockSSLConfigurable();

  private SSLParametersConfiguration configuration =
      new SSLParametersConfiguration();

  @Before
  public void setUp() throws Exception {
    configuration.setContext(new ContextBase());
  }

  @Test
  public void testSetIncludedProtocols() throws Exception {
    configurable.setSupportedProtocols(new String[] { "A", "B", "C", "D" });
    configuration.setIncludedProtocols("A,B ,C, D");
    configuration.configure(configurable);
    assertTrue(Arrays.equals(new String[] { "A", "B", "C", "D" },
        configurable.getEnabledProtocols()));
  }

  @Test
  public void testSetExcludedProtocols() throws Exception {
    configurable.setSupportedProtocols(new String[] { "A", "B" });
    configuration.setExcludedProtocols("A");
    configuration.configure(configurable);
    assertTrue(Arrays.equals(new String[] { "B" },
        configurable.getEnabledProtocols()));
  }

  @Test
  public void testSetIncludedAndExcludedProtocols() throws Exception {
    configurable.setSupportedProtocols(new String[] { "A", "B", "C" });
    configuration.setIncludedProtocols("A, B");
    configuration.setExcludedProtocols("B");
    configuration.configure(configurable);
    assertTrue(Arrays.equals(new String[] { "A" },
        configurable.getEnabledProtocols()));
  }

  @Test
  public void testSetIncludedCipherSuites() throws Exception {
    configurable.setSupportedCipherSuites(new String[] { "A", "B", "C", "D" });
    configuration.setIncludedCipherSuites("A,B ,C, D");
    configuration.configure(configurable);
    assertTrue(Arrays.equals(new String[] { "A", "B", "C", "D" },
        configurable.getEnabledCipherSuites()));
  }

  @Test
  public void testSetExcludedCipherSuites() throws Exception {
    configurable.setSupportedCipherSuites(new String[] { "A", "B" });
    configuration.setExcludedCipherSuites("A");
    configuration.configure(configurable);
    assertTrue(Arrays.equals(new String[]{ "B" },
        configurable.getEnabledCipherSuites()));
  }

  @Test
  public void testSetExcludedAndIncludedCipherSuites() throws Exception {
    configurable.setSupportedCipherSuites(new String[] { "A", "B", "C" });
    configuration.setIncludedCipherSuites("A, B");
    configuration.setExcludedCipherSuites("B");
    configuration.configure(configurable);
    assertTrue(Arrays.equals(new String[] { "A" },
        configurable.getEnabledCipherSuites()));
  }

  @Test
  public void testSetNeedClientAuth() throws Exception {
    configuration.setNeedClientAuth(true);
    configuration.configure(configurable);
    assertTrue(configurable.isNeedClientAuth());
  }

  @Test
  public void testSetWantClientAuth() throws Exception {
    configuration.setWantClientAuth(true);
    configuration.configure(configurable);
    assertTrue(configurable.isWantClientAuth());
  }

  @Test
  public void testPassDefaultProtocols() throws Exception {
    final String[] protocols = new String[] { "A" };
    configurable.setDefaultProtocols(protocols);
    configuration.configure(configurable);
    assertTrue(Arrays.equals(protocols, configurable.getEnabledProtocols()));
  }

  @Test
  public void testPassDefaultCipherSuites() throws Exception {
    final String[] cipherSuites = new String[] { "A" };
    configurable.setDefaultCipherSuites(cipherSuites);
    configuration.configure(configurable);
    assertTrue(Arrays.equals(cipherSuites,
        configurable.getEnabledCipherSuites()));
  }

  @Test
  public void testPassDefaultNeedClientAuth() throws Exception {
    configurable.setNeedClientAuth(true);
    configuration.configure(configurable);
    assertTrue(configurable.isNeedClientAuth());
  }

  @Test
  public void testPassDefaultWantClientAuth() throws Exception {
    configurable.setWantClientAuth(true);
    configuration.configure(configurable);
    assertTrue(configurable.isWantClientAuth());
  }

}
