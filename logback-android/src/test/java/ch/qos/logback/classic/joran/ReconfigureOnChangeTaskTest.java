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
package ch.qos.logback.classic.joran;

import static ch.qos.logback.classic.ClassicTestConstants.JORAN_INPUT_PREFIX;
import static ch.qos.logback.classic.joran.ReconfigureOnChangeTask.DETECTED_CHANGE_IN_CONFIGURATION_FILES;
import static ch.qos.logback.classic.joran.ReconfigureOnChangeTask.FALLING_BACK_TO_SAFE_CONFIGURATION;
import static ch.qos.logback.classic.joran.ReconfigureOnChangeTask.RE_REGISTERING_PREVIOUS_SAFE_CONFIGURATION;
import static ch.qos.logback.core.CoreConstants.RECONFIGURE_ON_CHANGE_TASK;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.issue.lbclassic135.LoggingRunnable;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.contention.AbstractMultiThreadedHarness;
import ch.qos.logback.core.contention.RunnableWithCounterAndDone;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.testUtil.FileTestUtil;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.util.StatusPrinter;

@RunWith(RobolectricTestRunner.class)
public class ReconfigureOnChangeTaskTest {
    final static int THREAD_COUNT = 5;

    int diff = RandomUtil.getPositiveInt();

    // the space in the file name mandated by
    // http://jira.qos.ch/browse/LBCORE-119
    final static String SCAN1_FILE_AS_STR = JORAN_INPUT_PREFIX + "roct/scan 1.xml";

    final static String SCAN_LOGBACK_474_FILE_AS_STR = JORAN_INPUT_PREFIX + "roct/scan_logback_474.xml";

    final static String INCLUSION_SCAN_TOPLEVEL0_AS_STR = JORAN_INPUT_PREFIX + "roct/inclusion/topLevel0.xml";

    final static String INCLUSION_SCAN_TOP_BY_RESOURCE_AS_STR = JORAN_INPUT_PREFIX + "roct/inclusion/topByResource.xml";

    final static String INCLUSION_SCAN_INNER0_AS_STR = JORAN_INPUT_PREFIX + "roct/inclusion/inner0.xml";

    final static String INCLUSION_SCAN_INNER1_AS_STR = ".+/asResource/inner1.xml$";

    private static final String SCAN_PERIOD_DEFAULT_FILE_AS_STR = JORAN_INPUT_PREFIX + "roct/scan_period_default.xml";

    LoggerContext loggerContext = new LoggerContext();
    Logger logger = loggerContext.getLogger(this.getClass());
    StatusChecker statusChecker = new StatusChecker(loggerContext);

    @BeforeClass
    static public void classSetup() {
        FileTestUtil.makeTestOutputDir();
    }


    void configure(File file) throws JoranException {
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(loggerContext);
        jc.doConfigure(file);
    }

    void configure(InputStream is) throws JoranException {
        JoranConfigurator jc = new JoranConfigurator();
        jc.setContext(loggerContext);
        jc.doConfigure(is);
    }

    @Test(timeout = 4000L)
    public void checkBasicLifecyle() throws JoranException, InterruptedException {
        File file = new File(SCAN1_FILE_AS_STR).getAbsoluteFile();
        configure(file);
        List<File> fileList = getConfigurationWatchList(loggerContext);
        assertThat(fileList, hasItems(file));
        checkThatTaskHasRan();
        checkThatTaskCanBeStopped();
    }

    private void checkThatTaskCanBeStopped() {
        ScheduledFuture<?> future = loggerContext.getScheduledFutures().get(0);
        loggerContext.stop();
        assertTrue(future.isCancelled());
    }

    private void checkThatTaskHasRan() throws InterruptedException {
        waitForReconfigureOnChangeTaskToRun();
    }

    List<File> getConfigurationWatchList(LoggerContext context) {
        ConfigurationWatchList configurationWatchList = ConfigurationWatchListUtil.getConfigurationWatchList(context);
        return configurationWatchList.getCopyOfFileWatchList();
    }

    @Test(timeout = 4000L)
    public void scanWithFileInclusion() throws JoranException, InterruptedException {
        File topLevelFile = new File(INCLUSION_SCAN_TOPLEVEL0_AS_STR).getAbsoluteFile();
        File innerFile = new File(INCLUSION_SCAN_INNER0_AS_STR).getAbsoluteFile();
        configure(topLevelFile);
        List<File> fileList = getConfigurationWatchList(loggerContext);
        assertThat(fileList, hasItems(topLevelFile, innerFile));
        checkThatTaskHasRan();
        checkThatTaskCanBeStopped();
    }

    @Test(timeout = 4000L)
    public void scanWithResourceInclusion() throws JoranException {
        File topLevelFile = new File(INCLUSION_SCAN_TOP_BY_RESOURCE_AS_STR).getAbsoluteFile();
        configure(topLevelFile);
        List<File> fileList = getConfigurationWatchList(loggerContext);
        List<String> filenameList = new ArrayList<String>();
        for (File f : fileList) {
            filenameList.add(f.getAbsolutePath());
        }
        assertThat(filenameList, hasItem(matchesPattern(INCLUSION_SCAN_INNER1_AS_STR)));
        assertThat(filenameList, hasItem(topLevelFile.getAbsolutePath()));
    }

    // See also http://jira.qos.ch/browse/LOGBACK-338
    @Test(timeout = 4000L)
    public void reconfigurationIsNotPossibleInTheAbsenceOfATopFile() throws IOException, JoranException {
        String configurationStr = "<configuration scan=\"true\" scanPeriod=\"50 millisecond\"><include resource=\"asResource/inner1.xml\"/></configuration>";
        configure(new ByteArrayInputStream(configurationStr.getBytes("UTF-8")));

        ConfigurationWatchList configurationWatchList = ConfigurationWatchListUtil.getConfigurationWatchList(loggerContext);
        assertNull(configurationWatchList);
        //assertNull(configurationWatchList.getMainURL());

        statusChecker.containsMatch(Status.WARN, "Due to missing top level");
        StatusPrinter.print(loggerContext);
        ReconfigureOnChangeTask roct = getRegisteredReconfigureTask();
        assertNull(roct);
        assertEquals(0, loggerContext.getScheduledFutures().size());
    }

    @Test(timeout = 3000L)
    public void fallbackToSafe_FollowedByRecovery() throws IOException, JoranException, InterruptedException {
        String path = CoreTestConstants.OUTPUT_DIR_PREFIX + "reconfigureOnChangeConfig_fallbackToSafe-" + diff + ".xml";
        File topLevelFile = new File(path);
        writeToFile(topLevelFile, "<configuration debug=\"true\" scan=\"true\" scanPeriod=\"5 millisecond\"><root level=\"ERROR\"/></configuration> ");
        configure(topLevelFile);
        CountDownLatch changeDetectedLatch = waitForReconfigurationToBeDone(null);
        ReconfigureOnChangeTask oldRoct = getRegisteredReconfigureTask();
        assertNotNull(oldRoct);
        writeToFile(topLevelFile, "<configuration debug=\"true\" scan=\"true\" scanPeriod=\"5 millisecond\">\n" + "  <root></configuration>");
        changeDetectedLatch.await();

        statusChecker.assertContainsMatch(Status.WARN, FALLING_BACK_TO_SAFE_CONFIGURATION);
        statusChecker.assertContainsMatch(Status.INFO, RE_REGISTERING_PREVIOUS_SAFE_CONFIGURATION);

        loggerContext.getStatusManager().clear();

        CountDownLatch secondDoneLatch = waitForReconfigurationToBeDone(oldRoct);
        writeToFile(topLevelFile, "<configuration debug=\"true\" scan=\"true\" scanPeriod=\"5 millisecond\"><root level=\"ERROR\"/></configuration> ");
        secondDoneLatch.await();

        statusChecker.assertIsErrorFree();
        statusChecker.containsMatch(DETECTED_CHANGE_IN_CONFIGURATION_FILES);
    }

    @Test(timeout = 4000L)
    public void fallbackToSafeWithIncludedFile_FollowedByRecovery() throws IOException, JoranException, InterruptedException {
        String topLevelFileAsStr = CoreTestConstants.OUTPUT_DIR_PREFIX + "reconfigureOnChangeConfig_top-" + diff + ".xml";
        String innerFileAsStr = CoreTestConstants.OUTPUT_DIR_PREFIX + "reconfigureOnChangeConfig_inner-" + diff + ".xml";
        File topLevelFile = new File(topLevelFileAsStr);
        writeToFile(topLevelFile, "<configuration debug=\"true\" scan=\"true\" scanPeriod=\"5 millisecond\"><include file=\"" + innerFileAsStr
                + "\"/></configuration> ");

        File innerFile = new File(innerFileAsStr);
        writeToFile(innerFile, "<included><root level=\"ERROR\"/></included> ");
        configure(topLevelFile);

        CountDownLatch doneLatch = waitForReconfigurationToBeDone(null);
        ReconfigureOnChangeTask oldRoct = getRegisteredReconfigureTask();
        assertNotNull(oldRoct);
        writeToFile(innerFile, "<included>\n<root>\n</included>");
        doneLatch.await();

        statusChecker.assertContainsMatch(Status.WARN, FALLING_BACK_TO_SAFE_CONFIGURATION);
        statusChecker.assertContainsMatch(Status.INFO, RE_REGISTERING_PREVIOUS_SAFE_CONFIGURATION);

        loggerContext.getStatusManager().clear();

        CountDownLatch secondDoneLatch = waitForReconfigurationToBeDone(oldRoct);
        writeToFile(innerFile, "<included><root level=\"ERROR\"/></included> ");
        secondDoneLatch.await();

        statusChecker.assertIsErrorFree();
        statusChecker.containsMatch(DETECTED_CHANGE_IN_CONFIGURATION_FILES);
    }


    private ReconfigureOnChangeTask getRegisteredReconfigureTask() {
        return (ReconfigureOnChangeTask) loggerContext.getObject(RECONFIGURE_ON_CHANGE_TASK);
    }

    class RunMethodInvokedListener extends ReconfigureOnChangeTaskListener {
        CountDownLatch countDownLatch;

        RunMethodInvokedListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void enteredRunMethod() {
            countDownLatch.countDown();
        }
    };

    class ChangeDetectedListener extends ReconfigureOnChangeTaskListener {
        CountDownLatch countDownLatch;

        ChangeDetectedListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void changeDetected() {
            countDownLatch.countDown();
        }
    };

    class ReconfigurationDoneListener extends ReconfigureOnChangeTaskListener {
        CountDownLatch countDownLatch;

        ReconfigurationDoneListener(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void doneReconfiguring() {
            countDownLatch.countDown();
        }
    };



    private ReconfigureOnChangeTask waitForReconfigureOnChangeTaskToRun() throws InterruptedException {
        ReconfigureOnChangeTask roct = null;
        while(roct == null) {
            roct = getRegisteredReconfigureTask();
            Thread.yield();
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        roct.addListener(new RunMethodInvokedListener(countDownLatch));
        countDownLatch.await();
        return roct;
    }

    private CountDownLatch waitForReconfigurationToBeDone(ReconfigureOnChangeTask oldTask) throws InterruptedException {
        ReconfigureOnChangeTask roct = oldTask;
        while(roct == oldTask) {
            roct = getRegisteredReconfigureTask();
            Thread.yield();
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        roct.addListener(new ReconfigurationDoneListener(countDownLatch));
        return countDownLatch;
    }

    private RunnableWithCounterAndDone[] buildRunnableArray(File configFile, UpdateType updateType) {
        RunnableWithCounterAndDone[] rArray = new RunnableWithCounterAndDone[THREAD_COUNT];
        rArray[0] = new Updater(configFile, updateType);
        for (int i = 1; i < THREAD_COUNT; i++) {
            rArray[i] = new LoggingRunnable(logger);
        }
        return rArray;
    }

    @Test
    public void checkReconfigureTaskScheduledWhenDefaultScanPeriodUsed() throws JoranException {
        File file = new File(SCAN_PERIOD_DEFAULT_FILE_AS_STR);
        configure(file);

        final List<ScheduledFuture<?>> scheduledFutures = loggerContext.getScheduledFutures();
        assertFalse(scheduledFutures.isEmpty());
        StatusPrinter.print(loggerContext);
    }

    // check for deadlocks
    @Test(timeout = 4000L)
    public void scan_LOGBACK_474() throws JoranException, InterruptedException {
        loggerContext.setName("scan_LOGBACK_474");
        File file = new File(SCAN_LOGBACK_474_FILE_AS_STR);
        // StatusListenerConfigHelper.addOnConsoleListenerInstance(loggerContext, new OnConsoleStatusListener());
        configure(file);

        //ReconfigureOnChangeTask roct = waitForReconfigureOnChangeTaskToRun();

        int expectedResets = 2;
        Harness harness = new Harness(expectedResets);

        RunnableWithCounterAndDone[] runnableArray = buildRunnableArray(file, UpdateType.TOUCH);
        harness.execute(runnableArray);

        loggerContext.getStatusManager().add(new InfoStatus("end of execution ", this));
        StatusPrinter.print(loggerContext);
        checkResetCount(expectedResets);
    }

    private void checkResetCount(int expected) {
        StatusChecker checker = new StatusChecker(loggerContext);
        checker.assertIsErrorFree();

        int effectiveResets = checker.matchCount(CoreConstants.RESET_MSG_PREFIX);
        assertEquals(expected, effectiveResets);


        // String failMsg = "effective=" + effectiveResets + ", expected=" + expected;
        //
        // there might be more effective resets than the expected amount
        // since the harness may be sleeping while a reset occurs
        //assertTrue(failMsg, expected <= effectiveResets && (expected + 2) >= effectiveResets);

    }

    void addInfo(String msg, Object o) {
        loggerContext.getStatusManager().add(new InfoStatus(msg, o));
    }

    enum UpdateType {
        TOUCH, MALFORMED, MALFORMED_INNER
    }

    void writeToFile(File file, String contents) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(contents);
        fw.close();
        // on linux changes to last modified are not propagated if the
        // time stamp is near the previous time stamp hence the random delta
        file.setLastModified(System.currentTimeMillis()+RandomUtil.getPositiveInt());
    }

    class Harness extends AbstractMultiThreadedHarness {
        int changeCountLimit;

        Harness(int changeCount) {
            this.changeCountLimit = changeCount;
        }

        public void waitUntilEndCondition() throws InterruptedException {
            ReconfigureOnChangeTaskTest.this.addInfo("Entering " + this.getClass() + ".waitUntilEndCondition()", this);

            int changeCount = 0;
            ReconfigureOnChangeTask lastRoct = null;
            CountDownLatch countDownLatch = null;

            while (changeCount < changeCountLimit) {
                ReconfigureOnChangeTask roct = (ReconfigureOnChangeTask) loggerContext.getObject(RECONFIGURE_ON_CHANGE_TASK);
                if (lastRoct != roct && roct != null) {
                    lastRoct = roct;
                    countDownLatch = new CountDownLatch(1);
                    roct.addListener(new ChangeDetectedListener(countDownLatch));
                } else if (countDownLatch != null) {
                    countDownLatch.await();
                    countDownLatch = null;
                    changeCount++;
                }
                Thread.yield();
            }
            ReconfigureOnChangeTaskTest.this.addInfo("*****Exiting " + this.getClass() + ".waitUntilEndCondition()", this);
        }

    }

    class Updater extends RunnableWithCounterAndDone {
        File configFile;
        UpdateType updateType;

        // it actually takes time for Windows to propagate file modification changes
        // values below 100 milliseconds can be problematic the same propagation
        // latency occurs in Linux but is even larger (>600 ms)
        // final static int DEFAULT_SLEEP_BETWEEN_UPDATES = 60;

        int sleepBetweenUpdates = 100;

        Updater(File configFile, UpdateType updateType) {
            this.configFile = configFile;
            this.updateType = updateType;
        }

        Updater(File configFile) {
            this(configFile, UpdateType.TOUCH);
        }

        public void run() {
            while (!isDone()) {
                try {
                    Thread.sleep(sleepBetweenUpdates);
                } catch (InterruptedException e) {
                }
                if (isDone()) {
                    ReconfigureOnChangeTaskTest.this.addInfo("Exiting Updater.run()", this);
                    return;
                }
                counter++;
                ReconfigureOnChangeTaskTest.this.addInfo("Touching [" + configFile + "]", this);
                switch (updateType) {
                    case TOUCH:
                        touchFile();
                        break;
                    case MALFORMED:
                        try {
                            malformedUpdate();
                        } catch (IOException e) {
                            e.printStackTrace();
                            fail("malformedUpdate failed");
                        }
                        break;
                    case MALFORMED_INNER:
                        try {
                            malformedInnerUpdate();
                        } catch (IOException e) {
                            e.printStackTrace();
                            fail("malformedInnerUpdate failed");
                        }
                }
            }
            ReconfigureOnChangeTaskTest.this.addInfo("Exiting Updater.run()", this);
        }

        private void malformedUpdate() throws IOException {
            writeToFile(configFile, "<configuration scan=\"true\" scanPeriod=\"50 millisecond\">\n" + "  <root level=\"ERROR\">\n" + "</configuration>");
        }

        private void malformedInnerUpdate() throws IOException {
            writeToFile(configFile, "<included>\n" + "  <root>\n" + "</included>");
        }

        void touchFile() {
            configFile.setLastModified(System.currentTimeMillis());
        }
    }

}
