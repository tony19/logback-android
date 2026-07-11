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

import android.util.Log
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.encoder.LayoutWrappingEncoder
import ch.qos.logback.core.joran.spi.DefaultClass

/**
 * An appender that wraps the native Android logging mechanism (*logcat*);
 * redirects all logging requests to *logcat*.
 *
 * **Note:**
 * By default, this appender pushes all messages to *logcat* regardless
 * of *logcat*'s own filter settings (i.e., everything is printed). To disable
 * this behavior and enable filter-checking, set [checkLoggable] to `true`.
 * See the [Android Developer Guide](https://developer.android.com/tools/logcat)
 * for details on adjusting the *logcat* filter.
 *
 * @author Fred Eisele
 * @author Anthony Trinh
 */
public open class LogcatAppender : UnsynchronizedAppenderBase<ILoggingEvent>() {

    /**
     * The layout-wrapping encoder for this appender's *logcat* message.
     * This is a [PatternLayoutEncoder] in most configurations, but any
     * [LayoutWrappingEncoder] with a layout is accepted (issue #376).
     */
    @set:DefaultClass(PatternLayoutEncoder::class)
    public var encoder: LayoutWrappingEncoder<ILoggingEvent>? = null

    /**
     * The layout-wrapping encoder for this appender's *logcat* tag
     * (a [PatternLayoutEncoder] in most configurations); specify `null`
     * to automatically use the logger's name as the tag.
     *
     * The expanded text of the pattern must be less than 23 characters as
     * limited by Android. Layouts that exceed this limit are truncated,
     * and a star is appended to the tag to indicate this.
     *
     * The encoder result is limited to 22 characters plus a star to
     * indicate truncation (`%logger{0}` has no length limit in logback,
     * but `LogcatAppender` limits the length internally). For example,
     * if the encoder evaluated to `foo.foo.foo.foo.foo.bar.Test`,
     * the tag seen in Logcat would be: `foo.foo.foo.foo.foo.ba*`.
     * Note that `%logger{23}` yields more useful results
     * (in this case: `f.f.foo.foo.bar.Test`).
     */
    @set:DefaultClass(PatternLayoutEncoder::class)
    public var tagEncoder: LayoutWrappingEncoder<ILoggingEvent>? = null

    /**
     * Whether to ask Android before logging a message with a specific
     * tag and priority (i.e., calls `android.util.Log.isLoggable`).
     *
     * See [Log.isLoggable](https://developer.android.com/reference/android/util/Log#isLoggable(java.lang.String,%20int))
     */
    public var checkLoggable: Boolean = false

    /**
     * Checks that required parameters are set, and if everything is in order,
     * activates this appender.
     */
    override fun start() {
        if (this.encoder?.layout == null) {
            addError("No layout set for the appender named [$name].")
            return
        }

        // tag encoder is optional but needs a layout
        this.tagEncoder?.let { tagEncoder ->
            val layout = tagEncoder.layout

            if (layout == null) {
                addError("No tag layout set for the appender named [$name].")
                return
            }

            // prevent stack traces from showing up in the tag
            // (which could lead to very confusing error messages)
            if (layout is PatternLayout) {
                if (tagEncoder is PatternLayoutEncoder) {
                    val pattern = tagEncoder.pattern
                    if (!pattern.contains("%nopex")) {
                        // restarting the encoder rebuilds its layout from the new pattern
                        tagEncoder.stop()
                        tagEncoder.pattern = "$pattern%nopex"
                        tagEncoder.start()
                    }
                    (tagEncoder.layout as PatternLayout).setPostCompileProcessor(null)
                } else {
                    val pattern = layout.pattern
                    if (pattern != null && !pattern.contains("%nopex")) {
                        layout.stop()
                        layout.setPostCompileProcessor(null)
                        layout.pattern = "$pattern%nopex"
                        layout.start()
                    }
                }
            }
        }

        super.start()
    }

    /**
     * Writes an event to Android's logging mechanism (logcat)
     *
     * @param event the event to be logged
     */
    public override fun append(event: ILoggingEvent) {
        if (!isStarted) {
            return
        }

        val tag = getTag(event)

        when (event.level.levelInt) {
            Level.ALL_INT, Level.TRACE_INT ->
                if (!checkLoggable || Log.isLoggable(tag, Log.VERBOSE)) {
                    Log.v(tag, layoutMessage(event))
                }

            Level.DEBUG_INT ->
                if (!checkLoggable || Log.isLoggable(tag, Log.DEBUG)) {
                    Log.d(tag, layoutMessage(event))
                }

            Level.INFO_INT ->
                if (!checkLoggable || Log.isLoggable(tag, Log.INFO)) {
                    Log.i(tag, layoutMessage(event))
                }

            Level.WARN_INT ->
                if (!checkLoggable || Log.isLoggable(tag, Log.WARN)) {
                    Log.w(tag, layoutMessage(event))
                }

            Level.ERROR_INT ->
                if (!checkLoggable || Log.isLoggable(tag, Log.ERROR)) {
                    Log.e(tag, layoutMessage(event))
                }

            else -> Unit // Level.OFF_INT and unknown levels
        }
    }

    /**
     * Gets the logcat tag string of a logging event
     *
     * @param event logging event to evaluate
     * @return the tag string, truncated if max length exceeded
     */
    protected open fun getTag(event: ILoggingEvent): String {
        // format tag based on encoder layout; truncate if max length
        // exceeded (only necessary for isLoggable(), which throws
        // IllegalArgumentException)
        val tag = this.tagEncoder?.layout?.doLayout(event) ?: event.loggerName
        return if (checkLoggable && tag.length > MAX_TAG_LENGTH) {
            "${tag.substring(0, MAX_TAG_LENGTH - 1)}*"
        } else {
            tag
        }
    }

    private fun layoutMessage(event: ILoggingEvent): String =
        this.encoder!!.layout.doLayout(event)

    private companion object {
        /**
         * Max tag length enforced by Android
         * https://developer.android.com/reference/android/util/Log#isLoggable(java.lang.String,%20int)
         */
        private const val MAX_TAG_LENGTH = 23
    }
}
