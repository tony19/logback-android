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
package ch.qos.logback.classic.issue.lbclassic135.lbclassic139;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;

public class LB139_DeadlockTest {

  LoggerContext loggerContext = new LoggerContext();

  @SuppressWarnings("deprecation")
  @Before
  public void setUp() {
    loggerContext.setName("LB139");
    BasicConfigurator bc = new BasicConfigurator();
    bc.setContext(loggerContext);
    bc.configure(loggerContext);
  }
  
  @Test //(timeout=3000)
  public void test() throws Exception {
    Worker worker = new Worker(loggerContext);
    Accessor accessor = new Accessor(worker, loggerContext);
    
    Thread workerThread = new Thread(worker, "WorkerThread");
    Thread accessorThread = new Thread(accessor, "AccessorThread");
    
    workerThread.start();
    accessorThread.start();

    int sleep = Worker.SLEEP_DUIRATION*10;
    
    System.out.println("Will sleep for "+sleep+" millis");
    Thread.sleep(sleep);
    System.out.println("Done sleeping ("+sleep+" millis)");
    worker.setDone(true);
    accessor.setDone(true);
    
    workerThread.join();
    accessorThread.join();
  }
}
