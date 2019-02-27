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
package ch.qos.logback.core.status;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.PrintStream;
import java.util.List;

/**
 *  Print all new incoming status messages on the on the designated PrintStream.
 * @author Ceki G&uuml;c&uuml;
 */
abstract public class OnPrintStreamStatusListenerBase extends ContextAwareBase implements StatusListener, LifeCycle {

  boolean isStarted = false;

  static final long DEFAULT_RETROSPECTIVE = 300;
  long retrospectiveThresold = DEFAULT_RETROSPECTIVE;

  /**
   * The prefix to place before each status message
   * @since 1.1.10
   */
  String prefix;

  /**
   * @return PrintStream used by derived classes
   */
  abstract protected PrintStream getPrintStream();

  private void print(Status status) {
    StringBuilder sb = new StringBuilder();

    if (prefix != null) {
      sb.append(prefix);
    }

    StatusPrinter.buildStr(sb, "", status);
    getPrintStream().print(sb);
  }

  public void addStatusEvent(Status status) {
    if (!isStarted)
      return;
    print(status);
  }

  /**
   * Print status messages retrospectively
   */
  private void retrospectivePrint() {
    if(context == null)
      return;
    long now = System.currentTimeMillis();
    StatusManager sm = context.getStatusManager();
    List<Status> statusList = sm.getCopyOfStatusList();
    for (Status status : statusList) {
      long timestampOfStatusMesage = status.getDate();
      if (isElapsedTimeLongerThanThreshold(now, timestampOfStatusMesage)) {
        print(status);
      }
    }
  }

  private boolean isElapsedTimeLongerThanThreshold(long now, long timestamp) {
    long elapsedTime = now - timestamp;
    return elapsedTime < retrospectiveThresold;
  }

  /**
   * Invoking the start method can cause the instance to print status messages created less than
   * value of retrospectiveThresold.
   */
  public void start() {
    isStarted = true;
    if (retrospectiveThresold > 0) {
      retrospectivePrint();
    }
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public void setRetrospective(long retrospective) {
    this.retrospectiveThresold = retrospective;
  }

  public long getRetrospective() {
    return retrospectiveThresold;
  }

  public void stop() {
    isStarted = false;
  }

  public boolean isStarted() {
    return isStarted;
  }

}
