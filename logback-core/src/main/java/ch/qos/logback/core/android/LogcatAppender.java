/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
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
package ch.qos.logback.core.android;

import android.util.Log;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * An appender that wraps the native Android logging mechanism (logcat).
 * 
 * @author Fred Eisele
 * @author Anthony Trinh
 */
public class LogcatAppender extends AppenderBase<ILoggingEvent> {

	private PatternLayoutEncoder encoder;
	private PatternLayoutEncoder tagEncoder = null;

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

		// format message based on encoder layout
		String msg = this.encoder.getLayout().doLayout(event);
		String tag = (this.tagEncoder != null) ? this.tagEncoder.getLayout().doLayout(event) : event.getLoggerName();
		
		switch (event.getLevel().levelInt) {
		case Level.ALL_INT:
		case Level.TRACE_INT:
			if (Log.isLoggable(tag, Log.VERBOSE)) {
				Log.v(tag, msg);
			}
			break;
		case Level.DEBUG_INT:
			if (Log.isLoggable(tag, Log.DEBUG)) {
				Log.d(tag, msg);
			}
			break;
		case Level.INFO_INT:
			if (Log.isLoggable(tag, Log.INFO)) {
				Log.i(tag, msg);
			}
			break;
		case Level.WARN_INT:
			if (Log.isLoggable(tag, Log.WARN)) {
				Log.w(tag, msg);
			}
			break;
		case Level.ERROR_INT:
			if (Log.isLoggable(tag, Log.ERROR)) {
				Log.e(tag, msg);
			}
			break;
		case Level.OFF_INT:
		default:
			break;
		}
	}

	public PatternLayoutEncoder getEncoder() {
		return this.encoder;
	}

	public void setEncoder(PatternLayoutEncoder encoder) {
		this.encoder = encoder;
	}

	public PatternLayoutEncoder getTagEncoder() {
		return this.tagEncoder;
	}

	public void setTagEncoder(PatternLayoutEncoder encoder) {
		this.tagEncoder = encoder;
	}

}
