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
package ch.qos.logback.classic.android

import org.slf4j.LoggerFactory

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.status.InfoStatus

/**
 * BasicLogcatConfigurator configures logback-classic by attaching a
 * [LogcatAppender] to the root logger. The appender's layout is set to a
 * [ch.qos.logback.classic.PatternLayout] with the pattern "%msg".
 *
 * The equivalent default configuration in XML would be:
 * ```
 * <configuration>
 *   <appender name="LOGCAT"
 *            class="ch.qos.logback.classic.android.LogcatAppender">
 *       <checkLoggable>false</checkLoggable>
 *       <encoder>
 *           <pattern>%msg</pattern>
 *       </encoder>
 *   </appender>
 *   <root level="DEBUG">
 *      <appender-ref ref="LOGCAT" />
 *   </root>
 * </configuration>
 * ```
 *
 * @author Anthony Trinh
 */
public object BasicLogcatConfigurator {

    @JvmStatic
    public fun configure(lc: LoggerContext) {
        lc.statusManager?.add(InfoStatus("Setting up default configuration.", lc))

        // We don't need a trailing new-line character in the pattern
        // because logcat automatically appends one for us.
        val encoder = PatternLayoutEncoder().apply {
            context = lc
            pattern = "%msg"
            start()
        }

        val appender = LogcatAppender().apply {
            context = lc
            name = "logcat"
            this.encoder = encoder
            start()
        }

        lc.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender)
    }

    @JvmStatic
    public fun configureDefaultContext() {
        configure(LoggerFactory.getILoggerFactory() as LoggerContext)
    }
}
