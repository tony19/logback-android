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
package ch.qos.logback.core.joran.action;

import org.xml.sax.Attributes;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.hook.DefaultShutdownHook;
import ch.qos.logback.core.hook.ShutdownHookBase;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Action which handles <shutdownHook> elements in configuration files.
 *
 * @author Mike Reinhold
 */
public class ShutdownHookAction extends Action {

    ShutdownHookBase hook;
    private boolean inError;

    /**
     * Instantiates a shutdown hook of the given class and sets its name.
     *
     * The hook thus generated is placed in the {@link InterpretationContext}'s
     * shutdown hook bag.
     */
    @Override
    public void begin(InterpretationContext ic, String name, Attributes attributes) throws ActionException {
        hook = null;
        inError = false;

        String className = attributes.getValue(CLASS_ATTRIBUTE);
        if (OptionHelper.isEmpty(className)) {
            className = DefaultShutdownHook.class.getName();
            addInfo("Assuming className [" + className + "]");
        }

        try {
            addInfo("About to instantiate shutdown hook of type [" + className + "]");

            hook = (ShutdownHookBase) OptionHelper.instantiateByClassName(className,
                    ShutdownHookBase.class, context);
            hook.setContext(context);

            ic.pushObject(hook);
        }catch (Exception e) {
            inError = true;
            addError("Could not create a shutdown hook of type [" + className + "].", e);
            throw new ActionException(e);
        }
    }

    /**
     * Once the children elements are also parsed, now is the time to activate the
     * shutdown hook options.
     */
    @Override
    public void end(InterpretationContext ic, String name) throws ActionException {
        if (inError) {
            return;
        }

        Object o = ic.peekObject();
        if (o != hook) {
            addWarn("The object at the of the stack is not the hook pushed earlier.");
        } else {
            ic.popObject();

            Thread hookThread = new Thread(hook, "Logback shutdown hook [" + context.getName() + "]");

            addInfo("Registering shutdown hook with JVM runtime");
            context.putObject(CoreConstants.SHUTDOWN_HOOK_THREAD, hookThread);
            Runtime.getRuntime().addShutdownHook(hookThread);
        }
    }
}
