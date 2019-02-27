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
 * A helper class that implements ContextAware methods. A class can implement
 * the ContextAware interface by deriving from this class.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class ContextAwareBase implements ContextAware {
  private int noContextWarning = 0;
  protected Context context;
  final Object declaredOrigin;

  public ContextAwareBase() {
    declaredOrigin = this;
  }
  public ContextAwareBase(ContextAware declaredOrigin) {
    this.declaredOrigin = declaredOrigin;
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

  /**
   * The declared origin of status messages. By default 'this'. Derived classes may override this
   * method to declare other origin.
   * 
   * @return the declared origin, by default 'this'
   */ 
  protected Object getDeclaredOrigin() {
    return declaredOrigin;
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
    addStatus(new InfoStatus(msg, getDeclaredOrigin()));
  }

  public void addInfo(String msg, Throwable ex) {
    addStatus(new InfoStatus(msg, getDeclaredOrigin(), ex));
  }

  public void addWarn(String msg) {
    addStatus(new WarnStatus(msg, getDeclaredOrigin()));
  }

  public void addWarn(String msg, Throwable ex) {
    addStatus(new WarnStatus(msg, getDeclaredOrigin(), ex));
  }

  public void addError(String msg) {
    addStatus(new ErrorStatus(msg, getDeclaredOrigin()));
  }

  public void addError(String msg, Throwable ex) {
    addStatus(new ErrorStatus(msg, getDeclaredOrigin(), ex));
  }
}
