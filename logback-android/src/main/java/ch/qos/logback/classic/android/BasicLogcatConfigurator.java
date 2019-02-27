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
package ch.qos.logback.classic.android;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;

/**
 * BasicLogcatConfigurator configures logback-classic by attaching a
 * {@link LogcatAppender} to the root logger. The appender's layout is set to a
 * {@link ch.qos.logback.classic.PatternLayout} with the pattern "%msg".
 *
 * The equivalent default configuration in XML would be:
 * <pre>
 * &lt;configuration&gt;
 *  &lt;appender name="LOGCAT"
 *           class="ch.qos.logback.classic.android.LogcatAppender" &gt;
 *      &lt;checkLoggable&gt;false&lt;/checkLoggable&gt;
 *      &lt;encoder&gt;
 *          &lt;pattern&gt;%msg&lt;/pattern&gt;
 *      &lt;/encoder&gt;
 *  &lt;/appender&gt;
 *  &lt;root level="DEBUG" &gt;
 *     &lt;appender-ref ref="LOGCAT" /&gt;
 *  &lt;/root&gt;
 * &lt;/configuration&gt;
 * </pre>
 *
 * @author Anthony Trinh
 */
public class BasicLogcatConfigurator {

  private BasicLogcatConfigurator() {
  }

  public static void configure(LoggerContext lc) {
    StatusManager sm = lc.getStatusManager();
    if (sm != null) {
      sm.add(new InfoStatus("Setting up default configuration.", lc));
    }
    LogcatAppender appender = new LogcatAppender();
    appender.setContext(lc);
    appender.setName("logcat");

    // We don't need a trailing new-line character in the pattern
    // because logcat automatically appends one for us.
    PatternLayoutEncoder pl = new PatternLayoutEncoder();
    pl.setContext(lc);
    pl.setPattern("%msg");
    pl.start();

    appender.setEncoder(pl);
    appender.start();
    Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(appender);
  }

  public static void configureDefaultContext() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    configure(lc);
  }
}
