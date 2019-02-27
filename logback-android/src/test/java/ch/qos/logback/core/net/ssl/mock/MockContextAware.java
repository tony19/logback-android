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
package ch.qos.logback.core.net.ssl.mock;

import java.util.LinkedList;
import java.util.List;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * A {@link ContextAware} with instrumentation for unit testing.
 *
 * @author Carl Harris
 */
public class MockContextAware extends ContextAwareBase
    implements ContextAware {

  private final List<String> info = new LinkedList<String>();
  private final List<String> warn = new LinkedList<String>();
  private final List<String> error = new LinkedList<String>();

  @Override
  public void addInfo(String msg) {
    info.add(msg);
  }

  @Override
  public void addWarn(String msg) {
    warn.add(msg);
  }

  @Override
  public void addError(String msg) {
    error.add(msg);
  }

  public boolean hasInfoMatching(String regex) {
    return hasMatching(info, regex);
  }

  public boolean hasWarnMatching(String regex) {
    return hasMatching(info, regex);
  }

  public boolean hasErrorMatching(String regex) {
    return hasMatching(info, regex);
  }

  private boolean hasMatching(List<String> messages, String regex) {
    for (String message : messages) {
      if (message.matches(regex)) return true;
    }
    return false;
  }

}
