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
import ch.qos.logback.core.status.Status;


/**
 * An object which has a context and add methods for updating internal status messages.
 */
public interface ContextAware {

  void setContext(Context context);

  Context getContext();

  void addStatus(Status status);

  void addInfo(String msg);

  void addInfo(String msg, Throwable ex);

  void addWarn(String msg);

  void addWarn(String msg, Throwable ex);

  void addError(String msg);

  void addError(String msg, Throwable ex);

}
