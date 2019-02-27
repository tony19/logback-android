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
package integrator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * A BundleActivator which invokes slf4j loggers
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public class Activator implements BundleActivator {

  private BundleContext m_context = null;

  public void start(BundleContext context) {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    
    try {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(lc);
      // the context was probably already configured by default configuration 
      // rules
      lc.reset(); 
      configurator.doConfigure("src/test/input/osgi/simple.xml");
    } catch (JoranException je) {
       je.printStackTrace();
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);

    
    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("Activator.start()");
    m_context = context;
  }

  public void stop(BundleContext context) {
    m_context = null;
    Logger logger = LoggerFactory.getLogger(this.getClass());
    logger.info("Activator.stop");
  }

  public Bundle[] getBundles() {
    if (m_context != null) {
      return m_context.getBundles();
    }
    return null;
  }
}