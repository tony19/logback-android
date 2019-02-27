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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A
 *
 * @author Ceki G&uuml;c&uuml;
 */
public class ScenarioBasedCyclicBufferTrackerTest {

  CyclicBufferTrackerSimulator simulator;
  CyclicBufferTrackerSimulator.Parameters parameters = new CyclicBufferTrackerSimulator.Parameters();

  void verify() {
    CyclicBufferTracker<Object> at = simulator.realCBTracker;
    CyclicBufferTrackerT<Object> t_at = simulator.t_CBTracker;
    assertEquals(t_at.liveKeysAsOrderedList(), at.liveKeysAsOrderedList());
    assertEquals(t_at.lingererKeysAsOrderedList(), at.lingererKeysAsOrderedList());
  }

  @Before public void setUp() {
    parameters.keySpaceLen = 128;
    parameters.maxTimestampInc = (int)ComponentTracker.DEFAULT_TIMEOUT / 2;
  }

  @Test
  public void shortTest() {
    parameters.keySpaceLen = 64;
    parameters.maxTimestampInc = 500;
    parameters.simulationLength = 70;

    simulator = new CyclicBufferTrackerSimulator(parameters);
    simulator.buildScenario();
    simulator.simulate();
    verify();
  }

  @Test
  public void mediumTest() {
    parameters.simulationLength = 20000;

    simulator = new CyclicBufferTrackerSimulator(parameters);
    simulator.buildScenario();
    simulator.simulate();
    verify();
  }

  @Test
  public void longTest() {
    parameters.simulationLength = 100*1000;
    simulator = new CyclicBufferTrackerSimulator(parameters);
    simulator.buildScenario();
    simulator.simulate();
    verify();
  }
}
