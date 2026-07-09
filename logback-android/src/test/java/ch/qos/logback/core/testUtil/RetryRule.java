/**
 * Copyright 2026 Anthony Trinh
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
package ch.qos.logback.core.testUtil;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Retries a failing test up to the given number of attempts. Intended only
 * for tests that simulate wall-clock/rollover timing and are inherently
 * prone to rare scheduling races; do not use it to paper over real bugs.
 */
public class RetryRule implements TestRule {

  private final int maxAttempts;

  public RetryRule(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        Throwable lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
          try {
            base.evaluate();
            return;
          } catch (Throwable t) {
            lastFailure = t;
            System.err.println(description.getDisplayName()
                + ": attempt " + attempt + " of " + maxAttempts + " failed");
          }
        }
        throw lastFailure;
      }
    };
  }
}
