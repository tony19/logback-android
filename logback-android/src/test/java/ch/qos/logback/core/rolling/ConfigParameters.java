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
package ch.qos.logback.core.rolling;

class ConfigParameters {

    long simulatedTime;
    int maxHistory;
    int simulatedNumberOfPeriods;
    int startInactivity = -1;
    int numInactivityPeriods;
    String fileNamePattern;
    long periodDurationInMillis = TimeBasedRollingWithArchiveRemoval_Test.MILLIS_IN_DAY;
    long sizeCap;

    ConfigParameters(long simulatedTime) {
        this.simulatedTime = simulatedTime;
    }

    ConfigParameters maxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
        return this;
    }

    ConfigParameters simulatedNumberOfPeriods(int simulatedNumberOfPeriods) {
        this.simulatedNumberOfPeriods = simulatedNumberOfPeriods;
        return this;
    }

    ConfigParameters startInactivity(int startInactivity) {
        this.startInactivity = startInactivity;
        return this;
    }

    ConfigParameters numInactivityPeriods(int numInactivityPeriods) {
        this.numInactivityPeriods = numInactivityPeriods;
        return this;
    }

    ConfigParameters fileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
        return this;
    }

    ConfigParameters periodDurationInMillis(long periodDurationInMillis) {
        this.periodDurationInMillis = periodDurationInMillis;
        return this;
    }

    ConfigParameters sizeCap(long sizeCap) {
        this.sizeCap = sizeCap;
        return this;
    }
}
