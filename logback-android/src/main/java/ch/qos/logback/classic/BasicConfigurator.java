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
package ch.qos.logback.classic;

import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * BasicConfigurator configures logback-classic by attaching a
 * {@link ConsoleAppender} to the root logger. The console appender's layout
 * is set to a {@link ch.qos.logback.classic.layout.TTLLLayout TTLLLayout}.
 *
 * @author Ceki Gulcu
 * @deprecated See {@link ch.qos.logback.classic.android.BasicLogcatConfigurator}.
 */
@Deprecated
public class BasicConfigurator extends ContextAwareBase implements Configurator {

  public BasicConfigurator() {
  }

  public void configure(LoggerContext lc) {
    addInfo("Setting up default configuration.");
    ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<ILoggingEvent>();
    ca.setContext(lc);
    ca.setName("console");
    LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<ILoggingEvent>();
    encoder.setContext(lc);

    // same as 
    // PatternLayout layout = new PatternLayout();
    // layout.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
    TTLLLayout layout = new TTLLLayout();

    layout.setContext(lc);
    layout.start();
    encoder.setLayout(layout);

    ca.setEncoder(encoder);
    ca.start();
    Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(ca);
  }
}
