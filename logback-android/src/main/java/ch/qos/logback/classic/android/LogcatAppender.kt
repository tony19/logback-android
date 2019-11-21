/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.classic.android

import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase

/**
 * An appender that wraps the native Android logging mechanism (*logcat*);
 * redirects all logging requests to *logcat*
 *
 *
 * **Note:**<br></br>
 * By default, this appender pushes all messages to *logcat* regardless
 * of *logcat*'s own filter settings (i.e., everything is printed). To disable this
 * behavior and enable filter-checking, use [.setCheckLoggable].
 * See the Android Developer Guide for details on adjusting the *logcat* filter.
 *
 *
 * See http://developer.android.com/guide/developing/tools/adb.html#filteringoutput
 *
 * @author Fred Eisele
 * @author Anthony Trinh
 */
class LogcatAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {

    /**
     * The pattern-layout encoder for this appender's *logcat* message
     */
    var encoder: PatternLayoutEncoder? = null

    /**
     * The pattern-layout encoder for this appender's *logcat* tag
     *
     * The expanded text of the pattern must be less than 23 characters as
     * limited by Android. Layouts that exceed this limit are truncated,
     * and a star is appended to the tag to indicate this.
     *
     * The `tagEncoder` result is limited to 22 characters plus a star to
     * indicate truncation (`%logger{0}` has no length limit in logback,
     * but `LogcatAppender` limits the length internally). For example,
     * if the `tagEncoder` evaluated to `foo.foo.foo.foo.foo.bar.Test`,
     * the tag seen in Logcat would be: `foo.foo.foo.foo.foo.ba*`.
     * Note that `%logger{23}` yields more useful results
     * (in this case: `f.f.foo.foo.bar.Test`).
     *
     * Specify `null` to automatically use the logger's name as the tag
     */
    var tagEncoder: PatternLayoutEncoder? = null

    /**
     * Whether to ask Android before logging a message with a specific
     * tag and priority (i.e., calls `android.util.Log.html#isLoggable`).
     *
     * See http://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String, int)
     *
     * @param enable
     * `true` to enable; `false` to disable
     */
    var checkLoggable = false

    /**
     * Checks that required parameters are set, and if everything is in order,
     * activates this appender.
     */
    override fun start() {
        if (this.encoder == null || this.encoder!!.layout == null) {
            addError("No layout set for the appender named [$name].")
            return
        }

        // tag encoder is optional but needs a layout
        if (this.tagEncoder != null) {
            val layout = this.tagEncoder!!.layout

            if (layout == null) {
                addError("No tag layout set for the appender named [$name].")
                return
            }

            // prevent stack traces from showing up in the tag
            // (which could lead to very confusing error messages)
            if (layout is PatternLayout) {
                val pattern = this.tagEncoder!!.pattern
                if (!pattern.contains("%nopex")) {
                    this.tagEncoder!!.stop()
                    this.tagEncoder!!.pattern = "$pattern%nopex"
                    this.tagEncoder!!.start()
                }

                layout.setPostCompileProcessor(null)
            }
        }

        super.start()
    }

    /**
     * Writes an event to Android's logging mechanism (logcat)
     *
     * @param event
     * the event to be logged
     */
    public override fun append(event: ILoggingEvent) {

        if (!isStarted) {
            return
        }

        val tag = getTag(event)

        when (event.level.levelInt) {
            Level.ALL_INT, Level.TRACE_INT -> if (!checkLoggable || Log.isLoggable(tag, Log.VERBOSE)) {
                Log.v(tag, this.encoder!!.layout.doLayout(event))
            }

            Level.DEBUG_INT -> if (!checkLoggable || Log.isLoggable(tag, Log.DEBUG)) {
                Log.d(tag, this.encoder!!.layout.doLayout(event))
            }

            Level.INFO_INT -> if (!checkLoggable || Log.isLoggable(tag, Log.INFO)) {
                Log.i(tag, this.encoder!!.layout.doLayout(event))
            }

            Level.WARN_INT -> if (!checkLoggable || Log.isLoggable(tag, Log.WARN)) {
                Log.w(tag, this.encoder!!.layout.doLayout(event))
            }

            Level.ERROR_INT -> if (!checkLoggable || Log.isLoggable(tag, Log.ERROR)) {
                Log.e(tag, this.encoder!!.layout.doLayout(event))
            }
        }
    }

    /**
     * Gets the logcat tag string of a logging event
     * @param event logging event to evaluate
     * @return the tag string, truncated if max length exceeded
     */
    fun getTag(event: ILoggingEvent): String {
        // format tag based on encoder layout; truncate if max length
        // exceeded (only necessary for isLoggable(), which throws
        // IllegalArgumentException)
        var tag = if (this.tagEncoder != null) this.tagEncoder!!.layout.doLayout(event) else event.loggerName
        if (checkLoggable && tag.length > MAX_TAG_LENGTH) {
            tag = tag.substring(0, MAX_TAG_LENGTH - 1) + "*"
        }
        return tag
    }

    companion object {
        /**
         * Max tag length enforced by Android
         * http://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String, int)
         */
        private const val MAX_TAG_LENGTH = 23
    }
}
