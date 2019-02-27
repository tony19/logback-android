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
package ch.qos.logback.core.spi;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;


/**
 * A helper class that implements ContextAware methods. Use this class to
 * implement the ContextAware interface by composition.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class ContextAwareImpl implements ContextAware {

  private int noContextWarning = 0;
  protected Context context;
  final Object origin;
  
  public ContextAwareImpl(Context context, Object origin) {
    this.context = context;
    this.origin = origin;

  }
  
  protected Object getOrigin() {
    return origin;
  }
  
  public void setContext(Context context) {
    if (this.context == null) {
      this.context = context;
    } else if (this.context != context) {
      throw new IllegalStateException("Context has been already set");
    }
  }

  public Context getContext() {
    return this.context;
  }

  public StatusManager getStatusManager() {
    if (context == null) {
      return null;
    }
    return context.getStatusManager();
  }

  public void addStatus(Status status) {
    if (context == null) {
      if (noContextWarning++ == 0) {
        System.out.println("LOGBACK: No context given for " + this);
      }
      return;
    }
    StatusManager sm = context.getStatusManager();
    if (sm != null) {
      sm.add(status);
    }
  }

  public void addInfo(String msg) {
    addStatus(new InfoStatus(msg, getOrigin()));
  }

  public void addInfo(String msg, Throwable ex) {
    addStatus(new InfoStatus(msg, getOrigin(), ex));
  }

  public void addWarn(String msg) {
    addStatus(new WarnStatus(msg, getOrigin()));
  }

  public void addWarn(String msg, Throwable ex) {
    addStatus(new WarnStatus(msg, getOrigin(), ex));
  }

  public void addError(String msg) {
    addStatus(new ErrorStatus(msg, getOrigin()));
  }

  public void addError(String msg, Throwable ex) {
    addStatus(new ErrorStatus(msg, getOrigin(), ex));
  }

}
