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
package ch.qos.logback.core.hook;

import ch.qos.logback.core.util.Duration;

/**
 * ShutdownHook implementation that stops the Logback context after a specified
 * delay. The default delay is 0 ms (zero).
 *
 * @author Mike Reinhold
 */
public class DefaultShutdownHook extends ShutdownHookBase {
    /**
     * Default delay before shutdown. The default is 0 (immediate).
     */
    public static final Duration DEFAULT_DELAY = Duration.buildByMilliseconds(0);

    /**
     * The delay in milliseconds before the ShutdownHook stops the
     * logback context
     */
    private Duration delay = DEFAULT_DELAY;

    public Duration getDelay() {
        return delay;
    }

    /**
     * The duration to wait before shutting down the current
     * logback context.
     * @param delay
     */
    public void setDelay(Duration delay) {
        this.delay = delay;
    }

    public void run() {
        if (delay.getMilliseconds() > 0) {
            addInfo("Sleeping for " + delay);
            try {
                Thread.sleep(delay.getMilliseconds());
            } catch (InterruptedException e) {
            }
        }
        super.stop();
    }
}
