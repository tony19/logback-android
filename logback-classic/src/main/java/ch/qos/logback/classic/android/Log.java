/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (c) 2013 Noveo Group
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * Simple Android-like adapter of {@link Logger} interface.
 * <p/>
 * Any call like {@code Log.someMethod(arguments)} is equal to:
 * <p/>
 * <code>
 * {@link #getLogger()}.someMethod(arguments);
 * </code>
 * <p/>
 * <b>Note</b>:
 * Do not forget to configure LOGBack before use.
 *
 * @author Pavel Stepanov
 */
public class Log {

  /**
   * Returns caller's {@link StackTraceElement}.
   *
   * @param aClass a class used as starting point to find a caller.
   * @return the caller stack trace element.
   */
  private static StackTraceElement getCaller(Class<?> aClass) {
    String className = aClass.getName();

    boolean packageFound = false;
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (StackTraceElement stackTraceElement : stackTrace) {
      if (!packageFound) {
        if (stackTraceElement.getClassName().equals(className)) {
          packageFound = true;
        }
      } else {
        if (!stackTraceElement.getClassName().equals(className)) {
          return stackTraceElement;
        }
      }
    }
    return stackTrace[stackTrace.length - 1];
  }

  /**
   * Returns caller's class name.
   *
   * @param aClass a class used as starting point to find a caller.
   * @return the class name of a caller.
   */
  private static String getCallerClassName(Class<?> aClass) {
    return getCaller(aClass).getClassName();
  }

  /**
   * Return a logger named corresponding to the class called this method,
   * using the statically bound {@link org.slf4j.ILoggerFactory} instance.
   *
   * @return logger
   */
  public static Logger getLogger() {
    return LoggerFactory.getLogger(getCallerClassName(Log.class));
  }

    /* *** *** *** *** *** *** *** *** *** *** *** *** */

  /**
   * Is the logger instance enabled for the VERBOSE level?
   *
   * @return {@code true} if this Logger is enabled for the VERBOSE level, false otherwise.
   */
  public static boolean isVerboseEnabled() {
    return getLogger().isTraceEnabled();
  }

  /**
   * Log a message at the VERBOSE level.
   *
   * @param msg the message string to be logged.
   */
  public static void v(String msg) {
    getLogger().trace(msg);
  }

  /**
   * Log a message at the VERBOSE level according to the specified format and argument.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the VERBOSE level.
   *
   * @param format the format string
   * @param arg    the argument
   */
  public static void v(String format, Object arg) {
    getLogger().trace(format, arg);
  }

  /**
   * Log a message at the VERBOSE level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the VERBOSE level.
   *
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void v(String format, Object arg1, Object arg2) {
    getLogger().trace(format, arg1, arg2);
  }

  /**
   * Log a message at the VERBOSE level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous string concatenation when the logger is disabled for the VERBOSE level.
   * However, this variant incurs the hidden (and relatively small) cost of creating an Object[] before
   * invoking the method, even if this logger is disabled for VERBOSE. The variants taking one and two
   * arguments exist solely in order to avoid this hidden cost.
   *
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void v(String format, Object... arguments) {
    getLogger().trace(format, arguments);
  }

  /**
   * Log an exception (throwable) at the VERBOSE level with an accompanying message.
   *
   * @param msg the message accompanying the exception.
   * @param t   the exception (throwable) to log.
   */
  public static void v(String msg, Throwable t) {
    getLogger().trace(msg, t);
  }

  /**
   * This method is similar to {@link #isDebugEnabled()}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @return {@code true} if this Logger is enabled for the VERBOSE level, false otherwise.
   */
  public static boolean isVerboseEnabled(Marker marker) {
    return getLogger().isTraceEnabled(marker);
  }

  /**
   * This method is similar to {@link #v(String)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @param msg    the message string to be logged.
   */
  public static void v(Marker marker, String msg) {
    getLogger().trace(marker, msg);
  }

  /**
   * This method is similar to {@link #v(String, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public static void v(Marker marker, String format, Object arg) {
    getLogger().trace(marker, format, arg);
  }

  /**
   * This method is similar to {@link #v(String, Object, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void v(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().trace(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #v(String, Object...)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker    the marker data specific to this log statement.
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void v(Marker marker, String format, Object... arguments) {
    getLogger().trace(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #v(String, Throwable)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param msg    the message accompanying the exception.
   * @param t      the exception (throwable) to log.
   */
  public static void v(Marker marker, String msg, Throwable t) {
    getLogger().trace(marker, msg, t);
  }

    /* *** *** *** *** *** *** *** *** *** *** *** *** */

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return {@code true} if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public static boolean isDebugEnabled() {
    return getLogger().isDebugEnabled();
  }

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged.
   */
  public static void d(String msg) {
    getLogger().debug(msg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format and argument.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the DEBUG level.
   *
   * @param format the format string
   * @param arg    the argument
   */
  public static void d(String format, Object arg) {
    getLogger().debug(format, arg);
  }

  /**
   * Log a message at the DEBUG level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the DEBUG level.
   *
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void d(String format, Object arg1, Object arg2) {
    getLogger().debug(format, arg1, arg2);
  }

  /**
   * Log a message at the DEBUG level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous string concatenation when the logger is disabled for the DEBUG level.
   * However, this variant incurs the hidden (and relatively small) cost of creating an Object[] before
   * invoking the method, even if this logger is disabled for DEBUG. The variants taking one and two
   * arguments exist solely in order to avoid this hidden cost.
   *
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void d(String format, Object... arguments) {
    getLogger().debug(format, arguments);
  }

  /**
   * Log an exception (throwable) at the DEBUG level with an accompanying message.
   *
   * @param msg the message accompanying the exception.
   * @param t   the exception (throwable) to log.
   */
  public static void d(String msg, Throwable t) {
    getLogger().debug(msg, t);
  }

  /**
   * This method is similar to {@link #isDebugEnabled()}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @return {@code true} if this Logger is enabled for the DEBUG level, false otherwise.
   */
  public static boolean isDebugEnabled(Marker marker) {
    return getLogger().isDebugEnabled(marker);
  }

  /**
   * This method is similar to {@link #d(String)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @param msg    the message string to be logged.
   */
  public static void d(Marker marker, String msg) {
    getLogger().debug(marker, msg);
  }

  /**
   * This method is similar to {@link #d(String, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public static void d(Marker marker, String format, Object arg) {
    getLogger().debug(marker, format, arg);
  }

  /**
   * This method is similar to {@link #d(String, Object, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void d(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().debug(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #d(String, Object...)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker    the marker data specific to this log statement.
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void d(Marker marker, String format, Object... arguments) {
    getLogger().debug(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #d(String, Throwable)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param msg    the message accompanying the exception.
   * @param t      the exception (throwable) to log.
   */
  public static void d(Marker marker, String msg, Throwable t) {
    getLogger().debug(marker, msg, t);
  }

    /* *** *** *** *** *** *** *** *** *** *** *** *** */

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return {@code true} if this Logger is enabled for the INFO level, false otherwise.
   */
  public static boolean isInfoEnabled() {
    return getLogger().isInfoEnabled();
  }

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged.
   */
  public static void i(String msg) {
    getLogger().info(msg);
  }

  /**
   * Log a message at the INFO level according to the specified format and argument.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the INFO level.
   *
   * @param format the format string
   * @param arg    the argument
   */
  public static void i(String format, Object arg) {
    getLogger().info(format, arg);
  }

  /**
   * Log a message at the INFO level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the INFO level.
   *
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void i(String format, Object arg1, Object arg2) {
    getLogger().info(format, arg1, arg2);
  }

  /**
   * Log a message at the INFO level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous string concatenation when the logger is disabled for the INFO level.
   * However, this variant incurs the hidden (and relatively small) cost of creating an Object[] before
   * invoking the method, even if this logger is disabled for INFO. The variants taking one and two
   * arguments exist solely in order to avoid this hidden cost.
   *
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void i(String format, Object... arguments) {
    getLogger().info(format, arguments);
  }

  /**
   * Log an exception (throwable) at the INFO level with an accompanying message.
   *
   * @param msg the message accompanying the exception.
   * @param t   the exception (throwable) to log.
   */
  public static void i(String msg, Throwable t) {
    getLogger().info(msg, t);
  }

  /**
   * This method is similar to {@link #isInfoEnabled()}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @return {@code true} if this Logger is enabled for the INFO level, false otherwise.
   */
  public static boolean isInfoEnabled(Marker marker) {
    return getLogger().isInfoEnabled(marker);
  }

  /**
   * This method is similar to {@link #i(String)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @param msg    the message string to be logged.
   */
  public static void i(Marker marker, String msg) {
    getLogger().info(marker, msg);
  }

  /**
   * This method is similar to {@link #i(String, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public static void i(Marker marker, String format, Object arg) {
    getLogger().info(marker, format, arg);
  }

  /**
   * This method is similar to {@link #i(String, Object, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void i(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().info(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #i(String, Object...)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker    the marker data specific to this log statement.
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void i(Marker marker, String format, Object... arguments) {
    getLogger().info(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #i(String, Throwable)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param msg    the message accompanying the exception.
   * @param t      the exception (throwable) to log.
   */
  public static void i(Marker marker, String msg, Throwable t) {
    getLogger().info(marker, msg, t);
  }

    /* *** *** *** *** *** *** *** *** *** *** *** *** */

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return {@code true} if this Logger is enabled for the WARN level, false otherwise.
   */
  public static boolean isWarnEnabled() {
    return getLogger().isWarnEnabled();
  }

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged.
   */
  public static void w(String msg) {
    getLogger().warn(msg);
  }

  /**
   * Log a message at the WARN level according to the specified format and argument.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the WARN level.
   *
   * @param format the format string
   * @param arg    the argument
   */
  public static void w(String format, Object arg) {
    getLogger().warn(format, arg);
  }

  /**
   * Log a message at the WARN level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the WARN level.
   *
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void w(String format, Object arg1, Object arg2) {
    getLogger().warn(format, arg1, arg2);
  }

  /**
   * Log a message at the WARN level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous string concatenation when the logger is disabled for the WARN level.
   * However, this variant incurs the hidden (and relatively small) cost of creating an Object[] before
   * invoking the method, even if this logger is disabled for WARN. The variants taking one and two
   * arguments exist solely in order to avoid this hidden cost.
   *
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void w(String format, Object... arguments) {
    getLogger().warn(format, arguments);
  }

  /**
   * Log an exception (throwable) at the WARN level with an accompanying message.
   *
   * @param msg the message accompanying the exception.
   * @param t   the exception (throwable) to log.
   */
  public static void w(String msg, Throwable t) {
    getLogger().warn(msg, t);
  }

  /**
   * This method is similar to {@link #isWarnEnabled()}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @return {@code true} if this Logger is enabled for the WARN level, false otherwise.
   */
  public static boolean isWarnEnabled(Marker marker) {
    return getLogger().isWarnEnabled(marker);
  }

  /**
   * This method is similar to {@link #w(String)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @param msg    the message string to be logged.
   */
  public static void w(Marker marker, String msg) {
    getLogger().warn(marker, msg);
  }

  /**
   * This method is similar to {@link #w(String, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public static void w(Marker marker, String format, Object arg) {
    getLogger().warn(marker, format, arg);
  }

  /**
   * This method is similar to {@link #w(String, Object, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void w(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().warn(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #w(String, Object...)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker    the marker data specific to this log statement.
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void w(Marker marker, String format, Object... arguments) {
    getLogger().warn(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #w(String, Throwable)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param msg    the message accompanying the exception.
   * @param t      the exception (throwable) to log.
   */
  public static void w(Marker marker, String msg, Throwable t) {
    getLogger().warn(marker, msg, t);
  }

    /* *** *** *** *** *** *** *** *** *** *** *** *** */

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return {@code true} if this Logger is enabled for the ERROR level, false otherwise.
   */
  public static boolean isErrorEnabled() {
    return getLogger().isErrorEnabled();
  }

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged.
   */
  public static void e(String msg) {
    getLogger().error(msg);
  }

  /**
   * Log a message at the ERROR level according to the specified format and argument.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the ERROR level.
   *
   * @param format the format string
   * @param arg    the argument
   */
  public static void e(String format, Object arg) {
    getLogger().error(format, arg);
  }

  /**
   * Log a message at the ERROR level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous object creation when the logger is disabled for the ERROR level.
   *
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void e(String format, Object arg1, Object arg2) {
    getLogger().error(format, arg1, arg2);
  }

  /**
   * Log a message at the ERROR level according to the specified format and arguments.
   * <p/>
   * This form avoids superfluous string concatenation when the logger is disabled for the ERROR level.
   * However, this variant incurs the hidden (and relatively small) cost of creating an Object[] before
   * invoking the method, even if this logger is disabled for ERROR. The variants taking one and two
   * arguments exist solely in order to avoid this hidden cost.
   *
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void e(String format, Object... arguments) {
    getLogger().error(format, arguments);
  }

  /**
   * Log an exception (throwable) at the ERROR level with an accompanying message.
   *
   * @param msg the message accompanying the exception.
   * @param t   the exception (throwable) to log.
   */
  public static void e(String msg, Throwable t) {
    getLogger().error(msg, t);
  }

  /**
   * This method is similar to {@link #isErrorEnabled()}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @return {@code true} if this Logger is enabled for the ERROR level, false otherwise.
   */
  public static boolean isErrorEnabled(Marker marker) {
    return getLogger().isErrorEnabled(marker);
  }

  /**
   * This method is similar to {@link #e(String)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker specific to this log statement.
   * @param msg    the message string to be logged.
   */
  public static void e(Marker marker, String msg) {
    getLogger().error(marker, msg);
  }

  /**
   * This method is similar to {@link #e(String, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement
   * @param format the format string
   * @param arg    the argument
   */
  public static void e(Marker marker, String format, Object arg) {
    getLogger().error(marker, format, arg);
  }

  /**
   * This method is similar to {@link #e(String, Object, Object)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param format the format string.
   * @param arg1   the first argument.
   * @param arg2   the second argument.
   */
  public static void e(Marker marker, String format, Object arg1, Object arg2) {
    getLogger().error(marker, format, arg1, arg2);
  }

  /**
   * This method is similar to {@link #e(String, Object...)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker    the marker data specific to this log statement.
   * @param format    the format string.
   * @param arguments a list of 3 or more arguments.
   */
  public static void e(Marker marker, String format, Object... arguments) {
    getLogger().error(marker, format, arguments);
  }

  /**
   * This method is similar to {@link #e(String, Throwable)}
   * method except that the marker data is also taken into consideration.
   *
   * @param marker the marker data specific to this log statement.
   * @param msg    the message accompanying the exception.
   * @param t      the exception (throwable) to log.
   */
  public static void e(Marker marker, String msg, Throwable t) {
    getLogger().error(marker, msg, t);
  }

}
