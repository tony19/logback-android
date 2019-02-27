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
package ch.qos.logback.core.util;

public interface InvocationGate {

  final long TIME_UNAVAILABLE = -1;

  /**
   * The caller of this method can decide to skip further work if the returned value is true.
   *
   * Implementations should be able to give a reasonable answer even if  current time date is unavailable.
   *
   * @param currentTime can be TIME_UNAVAILABLE (-1) to signal that time is not available
   * @return if true, caller should skip further work
   */
  public abstract boolean isTooSoon(long currentTime);

}
