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
package ch.qos.logback.classic;


import ch.qos.logback.classic.corpus.CorpusModel;
import ch.qos.logback.core.contention.*;
import org.junit.Before;
import org.junit.Test;

@org.junit.Ignore
public class LoggerContextPerfTest {

  static int THREAD_COUNT = 10000;
  int totalTestDuration = 4000;

  LoggerContext loggerContext = new LoggerContext();

  ThreadedThroughputCalculator harness = new ThreadedThroughputCalculator(totalTestDuration);
  RunnableWithCounterAndDone[] runnableArray = buildRunnableArray();

  CorpusModel corpusMaker;

  @Before
  public void setUp() throws Exception {
  }

  private RunnableWithCounterAndDone[] buildRunnableArray() {
    RunnableWithCounterAndDone[] runnableArray = new  RunnableWithCounterAndDone[THREAD_COUNT];
    for(int i = 0; i < THREAD_COUNT; i++) {
      runnableArray[i] = new GetLoggerRunnable();
    }
    return runnableArray;
  }

  // Results computed on a Intel i7
  // 1 thread
  // 13'107 ops per milli using Hashtable for LoggerContext.loggerCache
  // 15'258 ops per milli using ConcurrentHashMap for LoggerContext.loggerCache

  // 10 threads
  //  8'468 ops per milli using Hashtable for LoggerContext.loggerCache
  // 58'945 ops per milli using ConcurrentHashMap for LoggerContext.loggerCache

  // 100 threads
  //  8'863 ops per milli using Hashtable for LoggerContext.loggerCache
  // 34'810 ops per milli using ConcurrentHashMap for LoggerContext.loggerCache

  // 1'000 threads
  //  8'188 ops per milli using Hashtable for LoggerContext.loggerCache
  // 24'012 ops per milli using ConcurrentHashMap for LoggerContext.loggerCache

  // 10'000 threads
  // 7'595 ops per milli using Hashtable for LoggerContext.loggerCache
  // 8'989 ops per milli using ConcurrentHashMap for LoggerContext.loggerCache

  @Test
  public void computeResults() throws InterruptedException {
    harness.execute(runnableArray);
    harness.printThroughput("getLogger performance: ", true);
  }

  private class GetLoggerRunnable extends RunnableWithCounterAndDone {

    final int burstLength = 3;
    public void run() {
      while (!isDone()) {
        long i = counter % burstLength;

        loggerContext.getLogger("a"+i);
        counter++;
        if(i == 0) {
          Thread.yield();
        }
      }
    }
  }
}
