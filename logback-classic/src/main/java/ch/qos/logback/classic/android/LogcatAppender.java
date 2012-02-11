/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 2011, Anthony Trinh. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic.android;

import android.util.Log;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * An appender that wraps the native Android logging mechanism (<i>logcat</i>);
 * redirects all logging requests to <i>logcat</i> 
 * <p>
 * <b>Note:</b><br>
 * By default, this appender pushes all messages to <i>logcat</i> regardless
 * of <i>logcat</i>'s own filter settings (i.e., everything is printed). To disable this
 * behavior and enable filter-checking, use {@link #setCheckLoggable(boolean)}. 
 * See the Android Developer Guide for details on adjusting the <i>logcat</i> filter. 
 * 
 * @see http://developer.android.com/guide/developing/tools/adb.html#filteringoutput 
 * 
 * @author Fred Eisele
 * @author Anthony Trinh
 */
public class LogcatAppender extends AppenderBase<ILoggingEvent> {
	/**
	 * Max tag length enforced by Android 
	 * http://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String, int)
	 */
	private static final int MAX_TAG_LENGTH = 23;
	private PatternLayoutEncoder encoder = null;
	private PatternLayoutEncoder tagEncoder = null;
	private boolean checkLoggable = false;
	
	/**
	 * As in most cases, the default constructor does nothing.
	 */
	public LogcatAppender() {
	}

	/**
	 * Checks that required parameters are set, and if everything is in order,
	 * activates this appender.
	 */
	@Override
	public void start() {
		if ((this.encoder == null) || (this.encoder.getLayout() == null)) {
			addError("No layout set for the appender named [" + name + "].");
			return;
		}
		
		// tag encoder is optional but needs a layout
		if ((this.tagEncoder != null) && (this.tagEncoder.getLayout() == null)) {
			addError("No tag layout set for the appender named [" + name + "].");
			return;
		}
		
		super.start();
	}

	/**
	 * Writes an event to Android's logging mechanism (logcat)
	 * 
	 * @param event
	 *            the event to be logged
	 */
	public void append(ILoggingEvent event) {

		if (!isStarted()) {
			return;
		}

		// format tag based on encoder layout; truncate if max length 
		// exceeded (only necessary for isLoggable(), which throws 
		// IllegalArgumentException)
		String tag = (this.tagEncoder != null) ? this.tagEncoder.getLayout().doLayout(event) : event.getLoggerName();
		if (checkLoggable && (tag.length() > MAX_TAG_LENGTH)) {
			// addWarn("Truncating tag to " + MAX_TAG_LENGTH + " chars");
			tag = tag.substring(0, MAX_TAG_LENGTH - 1) + "*";
		}

		switch (event.getLevel().levelInt) {
		case Level.ALL_INT:
		case Level.TRACE_INT:
			if (!checkLoggable || Log.isLoggable(tag, Log.VERBOSE)) {
				Log.v(tag, this.encoder.getLayout().doLayout(event));
			}
			break;
		case Level.DEBUG_INT:
			if (!checkLoggable || Log.isLoggable(tag, Log.DEBUG)) {
				Log.d(tag, this.encoder.getLayout().doLayout(event));
			}
			break;
		case Level.INFO_INT:
			if (!checkLoggable || Log.isLoggable(tag, Log.INFO)) {
				Log.i(tag, this.encoder.getLayout().doLayout(event));
			}
			break;
		case Level.WARN_INT:
			if (!checkLoggable || Log.isLoggable(tag, Log.WARN)) {
				Log.w(tag, this.encoder.getLayout().doLayout(event));
			}
			break;
		case Level.ERROR_INT:
			if (!checkLoggable || Log.isLoggable(tag, Log.ERROR)) {
				Log.e(tag, this.encoder.getLayout().doLayout(event));
			}
			break;
		case Level.OFF_INT:
		default:
			break;
		}
	}

	/**
	 * Gets the pattern-layout encoder for this appender's <i>logcat</i> message
	 * 
	 * @return the pattern-layout encoder
	 */
	public PatternLayoutEncoder getEncoder() {
		return this.encoder;
	}

	/**
	 * Sets the pattern-layout encoder for this appender's <i>logcat</i> message
	 * 
	 * @param encoder the pattern-layout encoder
	 */
	public void setEncoder(PatternLayoutEncoder encoder) {
		this.encoder = encoder;
	}

	/**
	 * Gets the pattern-layout encoder for this appender's <i>logcat</i> tag
	 * 
	 * @return the pattern encoder
	 */
	public PatternLayoutEncoder getTagEncoder() {
		return this.tagEncoder;
	}

	/**
	 * Sets the pattern-layout encoder for this appender's <i>logcat</i> tag
	 * <p>
	 * The expanded text of the pattern must be less than 23 characters as
	 * limited by Android. Layouts that exceed this limit are truncated, 
	 * and a star is appended to the tag to indicate this.
	 * </p><p>
	 * The <code>tagEncoder</code> result is limited to 22 characters plus a star to 
	 * indicate truncation (<code>%logger{0}</code> has no length limit in logback, 
	 * but <code>LogcatAppender</code></a> limits the length internally). For example,
	 * if the <code>tagEncoder</code> evaluated to <code>foo.foo.foo.foo.foo.bar.Test</code>, 
	 * the tag seen in Logcat would be: <code>foo.foo.foo.foo.foo.ba*</code>. 
	 * Note that <code>%logger{23}</code> yields more useful results 
	 * (in this case: <code>f.f.foo.foo.bar.Test</code>).
	 * </p>
	 * 
	 * @param encoder
	 *            the pattern-layout encoder; specify {@code null} to
	 *            automatically use the logger's name as the tag
	 */
	public void setTagEncoder(PatternLayoutEncoder encoder) {
		this.tagEncoder = encoder;
	}

	/**
	 * Sets whether to ask Android before logging a message with a specific
	 * tag and priority (i.e., calls <a href="http://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String, int)"><code>isLoggable()</code>)</a>.
	 * 
	 * @param enable
	 * 			{@code true} to enable; {@code false} to disable
	 */
	public void setCheckLoggable(boolean enable) {
		this.checkLoggable = enable;
	}
	
	/**
	 * Gets the enable status of the <a href="http://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String, int)"><code>isLoggable()</code></a>-check
	 * that is called before logging
	 * 
	 * @return {@code true} if enabled; {@code false} otherwise
	 */
	public boolean getCheckLoggable() {
		return this.checkLoggable;
	}
}

/**
 * Stub class to run unit tests from command line
 */
/*
class Log {
	static public final int INFO = 1;
	static public final int WARN = 2;
	static public final int ERROR = 3;
	static public final int DEBUG = 4;
	static public final int VERBOSE = 5;
	
	static void v(String tag, String msg) {
	}
	static void d(String tag, String msg) {
	}
	static void i(String tag, String msg) {
	}
	static void w(String tag, String msg) {
	}
	static void e(String tag, String msg) {
	}
	static boolean isLoggable(String tag, int level) {
		return true;
	}
}
*/
