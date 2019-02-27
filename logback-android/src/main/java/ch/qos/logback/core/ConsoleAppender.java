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
package ch.qos.logback.core;

import java.io.OutputStream;
import java.util.Arrays;

import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.WarnStatus;

/**
 * ConsoleAppender appends log events to <code>System.out</code> or
 * <code>System.err</code> using a layout specified by the user. The default
 * target is <code>System.out</code>.
 * <p>
 * For more information about this appender, please refer to the online manual
 * at http://logback.qos.ch/manual/appenders.html#ConsoleAppender
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author Tom SH Liu
 * @author Ruediger Dohna
 * @deprecated This appender has been deprecated for {@link ch.qos.logback.classic.android.LogcatAppender}.
 */
@Deprecated
public class ConsoleAppender<E> extends OutputStreamAppender<E> {

  @SuppressWarnings("deprecation")
  protected ConsoleTarget target = ConsoleTarget.SystemOut;

  /**
   * Sets the value of the <b>Target</b> option. Recognized values are
   * "System.out" and "System.err". Any other value will be ignored.
   * @param value desired console-output target
   */
  public void setTarget(String value) {
    @SuppressWarnings("deprecation")
    ConsoleTarget t = ConsoleTarget.findByName(value.trim());
    if (t == null) {
      targetWarn(value);
    } else {
      target = t;
    }
  }

  /**
   * Returns the current value of the <b>target</b> property. The default value
   * of the option is "System.out".
   * <p>
   * See also {@link #setTarget}.
   * @return the console-output target's name
   */
  public String getTarget() {
    return target.getName();
  }

  private void targetWarn(String val) {
    Status status = new WarnStatus("[" + val + "] should be one of "
            + Arrays.toString(ConsoleTarget.values()), this);
    status.add(new WarnStatus(
            "Using previously set target, System.out by default.", this));
    addStatus(status);
  }

  @Override
  public void start() {
    OutputStream targetStream = target.getStream();
    setOutputStream(targetStream);
    super.start();
  }

}
