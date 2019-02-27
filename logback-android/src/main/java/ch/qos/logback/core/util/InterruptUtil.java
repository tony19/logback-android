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
package ch.qos.logback.core.util;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * Allows masking of interrupt flag if previously the flag is already set. Does nothing otherwise.
 *
 * Typical use:
 *
 * <pre>
 * InterruptUtil interruptUtil = new InterruptUtil(context);
 *
 * try {
 *   interruptUtil.maskInterruptFlag();
 *   someOtherThread.join(delay);
 * } catch(InterruptedException e) {
 *   // reachable only if join does not succeed within delay.
 *   // Without the maskInterruptFlag() call, the join() would have returned immediately
 *   // had the current thread been interrupted previously, i.e. before entering the above block
 * } finally {
 *   interruptUtil.unmaskInterruptFlag();
 * }
 * </pre>
 * @author Ceki Gulcu
 * @since 1.2.2
 */
public class InterruptUtil extends ContextAwareBase {

    final boolean previouslyInterrupted;

    public InterruptUtil(Context context) {
        super();
        setContext(context);
        previouslyInterrupted = Thread.currentThread().isInterrupted();
    }

    public void maskInterruptFlag() {
        if (previouslyInterrupted) {
            Thread.interrupted();
        }
    }

    public void unmaskInterruptFlag() {
        if (previouslyInterrupted) {
            try {
                Thread.currentThread().interrupt();
            } catch (SecurityException se) {
                addError("Failed to intrreupt current thread", se);
            }
        }
    }

}
