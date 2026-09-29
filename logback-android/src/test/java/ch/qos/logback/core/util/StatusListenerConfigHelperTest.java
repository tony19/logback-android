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

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;
import ch.qos.logback.core.status.StatusManager;

public class StatusListenerConfigHelperTest {

    Context context = new ContextBase();
    StatusManager sm = context.getStatusManager();
    String savedListenerClass;

    @Before
    public void setUp() throws Exception {
        savedListenerClass = System.getProperty(CoreConstants.STATUS_LISTENER_CLASS);
        System.clearProperty(CoreConstants.STATUS_LISTENER_CLASS);
    }

    @After
    public void tearDown() throws Exception {
        if (savedListenerClass == null) {
            System.clearProperty(CoreConstants.STATUS_LISTENER_CLASS);
        } else {
            System.setProperty(CoreConstants.STATUS_LISTENER_CLASS, savedListenerClass);
        }
        LifeCycleListener.INSTANCES.clear();
        QuietConsoleListener.INSTANCES.clear();
    }

    @Test
    public void addOnConsoleListenerInstanceShouldNotStartSecondListener() {
        OnConsoleStatusListener ocl0 = new OnConsoleStatusListener();
        OnConsoleStatusListener ocl1 = new OnConsoleStatusListener();

        StatusListenerConfigHelper.addOnConsoleListenerInstance(context, ocl0);
        {
            List<StatusListener> listeners = sm.getCopyOfStatusListenerList();
            assertEquals(1, listeners.size());
            assertTrue(ocl0.isStarted());
        }

        // second listener should not have been started
        StatusListenerConfigHelper.addOnConsoleListenerInstance(context, ocl1);
        {
            List<StatusListener> listeners = sm.getCopyOfStatusListenerList();
            assertEquals(1, listeners.size());
            assertFalse(ocl1.isStarted());
        }
    }

    @Test
    public void isInstantiable() {
        // the class only has static members, but its implicit constructor is public
        assertNotNull(new StatusListenerConfigHelper());
    }

    @Test
    public void installIfAskedDoesNothingWithoutProperty() {
        StatusListenerConfigHelper.installIfAsked(context);
        assertTrue(sm.getCopyOfStatusListenerList().isEmpty());
    }

    @Test
    public void installIfAskedAddsPlainListener() {
        System.setProperty(CoreConstants.STATUS_LISTENER_CLASS, PlainListener.class.getName());

        StatusListenerConfigHelper.installIfAsked(context);

        List<StatusListener> listeners = sm.getCopyOfStatusListenerList();
        assertEquals(1, listeners.size());
        assertTrue(listeners.get(0) instanceof PlainListener);
    }

    @Test
    public void installIfAskedSetsContextAndStartsLifeCycleListener() {
        System.setProperty(CoreConstants.STATUS_LISTENER_CLASS, LifeCycleListener.class.getName());

        StatusListenerConfigHelper.installIfAsked(context);

        assertEquals(1, LifeCycleListener.INSTANCES.size());
        LifeCycleListener listener = LifeCycleListener.INSTANCES.get(0);
        assertSame(context, listener.getContext());
        assertTrue(listener.isStarted());
        List<StatusListener> listeners = sm.getCopyOfStatusListenerList();
        assertEquals(1, listeners.size());
        assertSame(listener, listeners.get(0));
    }

    @Test
    public void installIfAskedDoesNotStartListenerRejectedByStatusManager() {
        System.setProperty(CoreConstants.STATUS_LISTENER_CLASS, QuietConsoleListener.class.getName());

        StatusListenerConfigHelper.installIfAsked(context);
        StatusListenerConfigHelper.installIfAsked(context);

        // the status manager keeps a single console listener per class
        assertEquals(2, QuietConsoleListener.INSTANCES.size());
        assertTrue(QuietConsoleListener.INSTANCES.get(0).isStarted());
        assertFalse(QuietConsoleListener.INSTANCES.get(1).isStarted());
        List<StatusListener> listeners = sm.getCopyOfStatusListenerList();
        assertEquals(1, listeners.size());
        assertSame(QuietConsoleListener.INSTANCES.get(0), listeners.get(0));
    }

    @Test
    public void installIfAskedPrintsFailureToCreateListener() {
        System.setProperty(CoreConstants.STATUS_LISTENER_CLASS, "no.such.StatusListener");
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(err, true));
        try {
            StatusListenerConfigHelper.installIfAsked(context);
        } finally {
            System.setErr(originalErr);
        }

        assertTrue(sm.getCopyOfStatusListenerList().isEmpty());
        String printed = err.toString();
        assertTrue(printed, printed.contains(DynamicClassLoadingException.class.getName()
                + ": Failed to instantiate type no.such.StatusListener"));
    }

    /** A status listener that is neither context-aware nor a life cycle. */
    public static class PlainListener implements StatusListener {
        @Override
        public void addStatusEvent(Status status) {
        }
    }

    /** A context-aware status listener with a life cycle. */
    public static class LifeCycleListener extends ContextAwareBase implements StatusListener, LifeCycle {
        static final List<LifeCycleListener> INSTANCES = new ArrayList<LifeCycleListener>();
        private boolean started;

        public LifeCycleListener() {
            INSTANCES.add(this);
        }

        @Override
        public void addStatusEvent(Status status) {
        }

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void stop() {
            started = false;
        }

        @Override
        public boolean isStarted() {
            return started;
        }
    }

    /** A console status listener that prints nowhere. */
    public static class QuietConsoleListener extends OnConsoleStatusListener {
        static final List<QuietConsoleListener> INSTANCES = new ArrayList<QuietConsoleListener>();

        public QuietConsoleListener() {
            INSTANCES.add(this);
        }

        @Override
        protected PrintStream getPrintStream() {
            return new PrintStream(new ByteArrayOutputStream());
        }
    }
}
