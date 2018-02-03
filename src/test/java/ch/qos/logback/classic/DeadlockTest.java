/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.classic;

import org.junit.Test;
import org.slf4j.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import ch.qos.logback.classic.android.BasicLogcatConfigurator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for a deadlock in {@link ch.qos.logback.classic.android.LogcatAppender} (Issue #104)
 */
public class DeadlockTest {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DeadlockTest.class);

    static {
        BasicLogcatConfigurator.configureDefaultContext();
    }

    @Test
    public void noDeadlock() throws InterruptedException {
        startThreads();
        Thread.sleep(4000);
        assertThat(deadlockExists(), is(false));
    }

    public static class A {
        private String mName;
        public synchronized String getName() {
            return mName;
        }
        public synchronized void setName(String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return "My name is " + getName();
        }

        public synchronized void doStuff(int count) {
            log.debug("A is doing some synchronized stuff, count={}", count);
        }
    }

    private boolean deadlockExists() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        // Returns null if no threads are deadlocked.
        long[] threadIds = bean.findDeadlockedThreads();

        if (threadIds != null) {
            ThreadInfo[] infos = bean.getThreadInfo(threadIds);

            for (ThreadInfo info : infos) {
                StackTraceElement[] stack = info.getStackTrace();
                // Log or store stack trace information.
            }
        }
        return threadIds != null;
    }

    private void startThreads() {
        final A a = new A();
        a.setName("BAR");

        new Thread("T1") {
            @Override
            public void run() {
                log.info("Thread 1 start");
                int count = 0;
                while (true) {
                    a.doStuff(++count);
                }
            }
        }.start();

        new Thread("T2") {
            @Override
            public void run() {
                log.info("Thread 2 start");
                int count = 0;
                while (true) {
                    log.debug("T2 count={} a={}", ++count, a);
                }
            }
        }.start();
    }
}
