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
package ch.qos.logback.core.recovery;

public class RecoveryCoordinator {

  public final static long BACKOFF_COEFFICIENT_MIN = 20;
  static long BACKOFF_COEFFICIENT_MAX = 327680;  // BACKOFF_COEFFICIENT_MIN * 4^7
  static long BACKOFF_MULTIPLIER = 4;
  private long backOffCoefficient = BACKOFF_COEFFICIENT_MIN;
  
  private static long UNSET = -1;
  // tests can set the time directly independently of system clock
  private long currentTime = UNSET;
  private long next;

  public RecoveryCoordinator() {
    next = getCurrentTime() + getBackoffCoefficient();
  }

  public RecoveryCoordinator(long currentTime) {
    this.currentTime = currentTime;
    next = getCurrentTime() + getBackoffCoefficient();
  }

  public boolean isTooSoon() {
    long now = getCurrentTime();
    if(now > next) {
      next = now + getBackoffCoefficient();
      return false;
    } else {
      return true;
    }
  }
  
  void setCurrentTime(long forcedTime) {
    currentTime = forcedTime;
  }
  
  private long getCurrentTime() {
    if(currentTime != UNSET) {
      return currentTime;
    }
    return System.currentTimeMillis();
  }
  
  private long getBackoffCoefficient() {
    long currentCoeff = backOffCoefficient;
    if(backOffCoefficient < BACKOFF_COEFFICIENT_MAX) {
      backOffCoefficient *= BACKOFF_MULTIPLIER;
    }
    return currentCoeff;
  }
}
