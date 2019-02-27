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
package ch.qos.logback.classic.net;

import java.security.NoSuchAlgorithmException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.net.ssl.ConfigurableSSLServerSocketFactory;
import ch.qos.logback.core.net.ssl.SSLParametersConfiguration;

/**
 * A {@link SimpleSocketServer} that supports SSL.
 *
 * <pre>
 *      &lt;b&gt;Usage:&lt;/b&gt; java ch.qos.logback.classic.net.ssl.SimpleSSLSocketServer port configFile
 * </pre>
 *
 * where <em>port</em> is a port number where the server listens and
 * <em>configFile</em> is an xml configuration file fed to
 * {@link JoranConfigurator}.
 *
 * When running the SimpleSSLServerFactory as shown above, it is necessary to
 * configure JSSE system properties using {@code -Dname=value} on the
 * command-line when starting the server. In particular, you will probably
 * want/need to configure the following system properties:
 * <ul>
 * <li>javax.net.ssl.keyStore</li>
 * <li>javax.net.ssl.keyStorePassword</li>
 * <li>javax.net.ssl.keyStoreType</li>
 * <li>javax.net.ssl.trustStore</li>
 * <li>javax.net.ssl.trustStorePassword</li>
 * <li>javax.net.ssl.trustStoreType</li>
 * </ul>
 * <p>
 * See the <a href=
 * "http://docs.oracle.com/javase/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#InstallationAndCustomization">
 * Customizing the JSSE</a> in the JSSE Reference Guide for details on how to
 * set these system properties.
 *
 * @author Carl Harris
 */
public class SimpleSSLSocketServer extends SimpleSocketServer {

  private final ServerSocketFactory socketFactory;

  public static void main(String argv[]) throws Exception {
    doMain(SimpleSSLSocketServer.class, argv);
  }

  /**
   * Creates a new server using the default SSL context.
   * @param lc logger context for received events
   * @param port port on which the server is to listen
   * @throws NoSuchAlgorithmException if the default SSL context cannot be
   *         created
   */
  public SimpleSSLSocketServer(LoggerContext lc, int port)
      throws NoSuchAlgorithmException {
    this(lc, port, SSLContext.getDefault());
  }

  /**
   * Creates a new server using a custom SSL context.
   * @param lc logger context for received events
   * @param port port on which the server is to listen
   * @param sslContext custom SSL context
   */
  public SimpleSSLSocketServer(LoggerContext lc, int port,
      SSLContext sslContext) {
    super(lc, port);
    if (sslContext == null) {
      throw new NullPointerException("SSL context required");
    }
    SSLParametersConfiguration parameters = new SSLParametersConfiguration();

    parameters.setContext(lc);
    this.socketFactory = new ConfigurableSSLServerSocketFactory(
        parameters, sslContext.getServerSocketFactory());
  }

  @Override
  protected ServerSocketFactory getServerSocketFactory() {
    return socketFactory;
  }

}
