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
package ch.qos.logback.core.net.mock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ExecutorService} with instrumentation for unit testing.
 * <p>
 * This service is synchronous; submitted jobs are run on the calling thread.
 *
 * @author Carl Harris
 */
public class MockExecutorService extends AbstractExecutorService {

  private Runnable lastCommand;

  public Runnable getLastCommand() {
    return lastCommand;
  }

  public void shutdown() {
  }

  public List<Runnable> shutdownNow() {
    return Collections.emptyList();
  }

  public boolean isShutdown() {
    return true;
  }

  public boolean isTerminated() {
    return true;
  }

  public boolean awaitTermination(long timeout, TimeUnit unit)
      throws InterruptedException {
    return true;
  }

  public void execute(Runnable command) {
    command.run();
    lastCommand = command;
  }

}
