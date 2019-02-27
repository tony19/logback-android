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

import ch.qos.logback.core.helpers.CyclicBuffer;

import java.util.*;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class CyclicBufferTrackerSimulator {

  static class Parameters {
    public int keySpaceLen;
    public int maxTimestampInc;
    public int simulationLength;
  }

  CyclicBufferTracker<Object> realCBTracker = new CyclicBufferTracker<Object>();
  CyclicBufferTrackerT<Object> t_CBTracker = new CyclicBufferTrackerT<Object>();

  List<SimulationEvent> scenario = new ArrayList<SimulationEvent>();
  List<String> keySpace = new ArrayList<String>();
  Random randomKeyGen = new Random(100);
  Random simulatorRandom = new Random(11234);
  Parameters params;

  int getToEndOfLifeRatio = 10;

  CyclicBufferTrackerSimulator(Parameters params) {
    this.params = params;
    Map<String, String> checkMap = new HashMap<String, String>();
    for (int i = 0; i < params.keySpaceLen; i++) {
      String k = getRandomKeyStr();
      if (checkMap.containsKey(k)) {
        System.out.println("random key collision occurred");
        k += "" + i;
      }
      keySpace.add(k);
      checkMap.put(k, k);
    }

  }

  private String getRandomKeyStr() {
    int ri = randomKeyGen.nextInt();
    return String.format("%X", ri);
  }

  void buildScenario() {
    long timestamp = 30000;
    int keySpaceLen = keySpace.size();
    for (int i = 0; i < params.simulationLength; i++) {
      int keyIndex = simulatorRandom.nextInt(keySpaceLen);
      timestamp += simulatorRandom.nextInt(params.maxTimestampInc);
      String key = keySpace.get(keyIndex);
      scenario.add(new SimulationEvent(EventType.INSERT, key, timestamp));
      if (simulatorRandom.nextInt(getToEndOfLifeRatio) == 0) {
        scenario.add(new SimulationEvent(EventType.END_OF_LIFE, key, timestamp));
      }
      scenario.add(new SimulationEvent(EventType.REMOVE_STALE, key, timestamp));
    }
  }

  public void dump() {
    for (SimulationEvent simeEvent : scenario) {
      System.out.println(simeEvent);
    }
  }


  void play(SimulationEvent simulationEvent,
            ComponentTracker<CyclicBuffer<Object>> tracker) {
    String key = simulationEvent.key;
    long timestamp = simulationEvent.timestamp;
    EventType eventType = simulationEvent.eventType;
    switch (eventType) {
      case INSERT:
        tracker.getOrCreate(key, timestamp);
        break;
      case END_OF_LIFE:
        tracker.endOfLife(key);
        break;
      case REMOVE_STALE:
        tracker.removeStaleComponents(timestamp);
        break;
    }
  }

  public void simulate() {
    for (SimulationEvent simeEvent : scenario) {
      play(simeEvent, realCBTracker);
      play(simeEvent, t_CBTracker);
    }
  }

  // =========================================================================
  enum EventType {
    INSERT, END_OF_LIFE, REMOVE_STALE;
  }

  class SimulationEvent {
    final public String key;
    final public long timestamp;
    final EventType eventType;

    public SimulationEvent(EventType eventType, String key, long timestamp) {
      this.eventType = eventType;
      this.key = key;
      this.timestamp = timestamp;
    }

    @Override
    public String toString() {
      return "SimulationEvent{" +
              "eventType=" + eventType +
              ", key='" + key + '\'' +
              ", timestamp=" + timestamp +
              '}';
    }
  }
}
