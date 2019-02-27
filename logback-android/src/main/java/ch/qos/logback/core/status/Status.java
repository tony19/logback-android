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

import java.util.Iterator;


public interface Status  {

  int INFO = 0;
  int WARN = 1;
  int ERROR = 2;
  
  int getLevel();
  int getEffectiveLevel();
  Object getOrigin();
  String getMessage();
  Throwable getThrowable();
  Long getDate();
  
  boolean hasChildren();
  void add(Status child);
  boolean remove(Status child);
  Iterator<Status> iterator();

}
