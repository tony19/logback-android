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

import static ch.qos.logback.classic.joran.ReconfigureOnChangeTask.DETECTED_CHANGE_IN_CONFIGURATION_FILES;
import static ch.qos.logback.classic.joran.ReconfigureOnChangeTask.FALLING_BACK_TO_SAFE_CONFIGURATION;
import static ch.qos.logback.classic.joran.ReconfigureOnChangeTask.RE_REGISTERING_PREVIOUS_SAFE_CONFIGURATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

/**
 * Plain-JVM tests of {@link ReconfigureOnChangeTask#run()} that drive the task
 * synchronously, without a scheduler, a real XML parser or file-modification
 * timing. The scheduled, end-to-end scenarios live in the Robolectric-run
 * {@link ReconfigureOnChangeTaskTest}.
 */
public class ReconfigureOnChangeTaskJvmTest {

    static final String MARKER_KEY = "rocTaskMarker";
    static final String NEW_CONFIGURATION_KEY = "rocTaskNewConfiguration";
    static final String NO_PREVIOUS_CONFIGURATION = "No previous configuration to fall back on.";
    static final String SAFE_CONFIGURATION_FAILED = "Unexpected exception thrown by a configuration considered safe.";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    LoggerContext loggerContext = new LoggerContext();
    StatusChecker statusChecker = new StatusChecker(loggerContext);
    ReconfigureOnChangeTask task = new ReconfigureOnChangeTask();
    List<String> notifications = new ArrayList<String>();

    @Before
    public void setUp() {
        loggerContext.setName("rocTaskJvm");
        // reset() clears the context properties: the marker tells whether a reconfiguration took place
        loggerContext.putProperty(MARKER_KEY, "present");
        task.setContext(loggerContext);
    }

    @Test
    public void runWithoutWatchListWarnsAndNotifiesEveryListenerOnlyOfEntry() {
        task.addListener(new RecordingListener("first"));
        task.addListener(new RecordingListener("second"));

        task.run();

        assertEquals(Arrays.asList("first:entered", "second:entered"), notifications);
        statusChecker.assertContainsMatch(Status.WARN, Pattern.quote("Empty ConfigurationWatchList in context"));
        statusChecker.assertNoMatch(Pattern.quote(DETECTED_CHANGE_IN_CONFIGURATION_FILES));
        assertContextWasNotReset();
    }

    @Test
    public void runWithNullWatchFileListDisablesScanningWithoutCheckingForChanges() throws IOException {
        StubWatchList watchList = new StubWatchList(xmlUrl("main.xml"), null, true);
        ConfigurationWatchListUtil.registerConfigurationWatchList(loggerContext, watchList);

        task.run();

        statusChecker.assertContainsMatch(Status.INFO, Pattern.quote("Empty watch file list. Disabling "));
        assertEquals(0, watchList.changeDetectedCalls);
        statusChecker.assertNoMatch(Pattern.quote(DETECTED_CHANGE_IN_CONFIGURATION_FILES));
        assertContextWasNotReset();
    }

    @Test
    public void runWithEmptyWatchFileListDisablesScanning() {
        ConfigurationWatchList watchList = new ConfigurationWatchList();
        watchList.setContext(loggerContext);
        ConfigurationWatchListUtil.registerConfigurationWatchList(loggerContext, watchList);
        task.addListener(new RecordingListener("l"));

        task.run();

        statusChecker.assertContainsMatch(Status.INFO, Pattern.quote("Empty watch file list. Disabling "));
        statusChecker.assertNoMatch(Pattern.quote(DETECTED_CHANGE_IN_CONFIGURATION_FILES));
        assertEquals(Collections.singletonList("l:entered"), notifications);
        assertContextWasNotReset();
    }

    @Test
    public void runAbortsReconfigurationFromGroovyFileWithAnError() throws IOException {
        URL groovyUrl = tmp.newFile("logback.groovy").toURI().toURL();
        registerChangedWatchList(groovyUrl);
        task.addListener(new RecordingListener("l"));

        task.run();

        statusChecker.assertContainsMatch(Status.INFO, Pattern.quote(DETECTED_CHANGE_IN_CONFIGURATION_FILES));
        statusChecker.assertContainsMatch(Status.INFO,
                Pattern.quote(CoreConstants.RESET_MSG_PREFIX + "named [rocTaskJvm]"));
        statusChecker.assertContainsMatch(Status.ERROR,
                Pattern.quote("Groovy classes are not available on the class path. ABORTING INITIALIZATION."));
        assertEquals(Arrays.asList("l:entered", "l:changeDetected", "l:done"), notifications);
        assertContextWasNotReset();
    }

    @Test
    public void runWithoutListenersIgnoresChangedFileThatIsNeitherXmlNorGroovy() throws IOException {
        URL propertiesUrl = tmp.newFile("logback.properties").toURI().toURL();
        registerChangedWatchList(propertiesUrl);

        task.run();

        statusChecker.assertContainsMatch(Status.INFO, Pattern.quote(DETECTED_CHANGE_IN_CONFIGURATION_FILES));
        statusChecker.assertContainsMatch(Status.INFO,
                Pattern.quote(CoreConstants.RESET_MSG_PREFIX + "named [rocTaskJvm]"));
        statusChecker.assertIsErrorFree();
        assertContextWasNotReset();
    }

    @Test
    public void unreadableMainXmlWithoutSafeConfigurationWarnsThatThereIsNothingToFallBackOn() throws IOException {
        URL missingXml = xmlUrl("missing.xml");
        registerChangedWatchList(missingXml);
        task.addListener(new RecordingListener("l"));

        task.run();

        assertNull("the context should have been reset", loggerContext.getProperty(MARKER_KEY));
        statusChecker.assertContainsMatch(Status.ERROR, Pattern.quote("Could not open URL [" + missingXml + "]."));
        statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(NO_PREVIOUS_CONFIGURATION));
        statusChecker.assertNoMatch(Pattern.quote(FALLING_BACK_TO_SAFE_CONFIGURATION));
        assertEquals(Arrays.asList("l:entered", "l:changeDetected", "l:done"), notifications);
    }

    @Test
    public void unreadableMainXmlFallsBackToSafeConfiguration() throws IOException {
        URL missingXml = xmlUrl("missing.xml");
        registerChangedWatchList(missingXml);
        List<SaxEvent> safeEvents = configurationWithRootLevel(Level.WARN);
        JoranConfigurator safeConfigurator = new JoranConfigurator();
        safeConfigurator.setContext(loggerContext);
        safeConfigurator.registerSafeConfiguration(safeEvents);
        assertEquals(Level.DEBUG, rootLevel());

        task.run();

        assertNull("the context should have been reset", loggerContext.getProperty(MARKER_KEY));
        statusChecker.assertContainsMatch(Status.ERROR, Pattern.quote("Could not open URL [" + missingXml + "]."));
        statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(FALLING_BACK_TO_SAFE_CONFIGURATION));
        statusChecker.assertContainsMatch(Status.INFO, Pattern.quote(RE_REGISTERING_PREVIOUS_SAFE_CONFIGURATION));
        statusChecker.assertNoMatch(Pattern.quote(NO_PREVIOUS_CONFIGURATION));
        // the safe configuration was replayed and registered once more
        assertEquals(Level.WARN, rootLevel());
        assertSame(safeEvents, loggerContext.getObject(CoreConstants.SAFE_JORAN_CONFIGURATION));
        // the watch list of the failed attempt is kept (as a copy) so that scanning goes on
        ConfigurationWatchList watchList = ConfigurationWatchListUtil.getConfigurationWatchList(loggerContext);
        assertNotNull(watchList);
        assertEquals(missingXml, watchList.getMainURL());
    }

    @Test
    public void failingSafeConfigurationIsReportedAsAnError() throws Exception {
        final URL mainXml = xmlUrl("main.xml");
        registerChangedWatchList(mainXml);
        final List<SaxEvent> safeEvents = configurationWithRootLevel(Level.WARN);
        final JoranException mainFailure = new JoranException("main configuration is broken");
        final JoranException safeFailure = new JoranException("safe configuration is broken");

        try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class,
                (mock, ctx) -> {
                    if (ctx.getCount() == 1) {
                        // the configurator of the main file: it registers the file and then fails
                        when(mock.recallSafeConfiguration()).thenReturn(safeEvents);
                        doAnswer(invocation -> {
                            JoranConfigurator.informContextOfURLUsedForConfiguration(loggerContext, mainXml);
                            throw mainFailure;
                        }).when(mock).doConfigure(mainXml);
                    } else {
                        // the configurator of the safe fallback events
                        doThrow(safeFailure).when(mock).doConfigure(anyList());
                    }
                })) {

            task.run();

            assertEquals(2, configurators.constructed().size());
            JoranConfigurator fallbackConfigurator = configurators.constructed().get(1);
            verify(fallbackConfigurator).setContext(loggerContext);
            verify(fallbackConfigurator).doConfigure(safeEvents);
            verify(fallbackConfigurator, never()).registerSafeConfiguration(any());
        }

        statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(FALLING_BACK_TO_SAFE_CONFIGURATION));
        Status error = findStatus(SAFE_CONFIGURATION_FAILED);
        assertEquals(Status.ERROR, error.getLevel());
        assertSame(safeFailure, error.getThrowable());
        statusChecker.assertNoMatch(Pattern.quote(RE_REGISTERING_PREVIOUS_SAFE_CONFIGURATION));
        // the fallback registered a copy of the watch list before replaying the safe events
        ConfigurationWatchList watchList = ConfigurationWatchListUtil.getConfigurationWatchList(loggerContext);
        assertEquals(mainXml, watchList.getMainURL());
    }

    @Test
    public void runLeavesTheContextAloneWhenNoChangeIsDetected() throws IOException {
        StubWatchList watchList = new StubWatchList(xmlUrl("main.xml"),
                Collections.singletonList(new File(tmp.getRoot(), "main.xml")), false);
        ConfigurationWatchListUtil.registerConfigurationWatchList(loggerContext, watchList);
        task.addListener(new RecordingListener("l"));

        try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class)) {
            task.run();

            assertEquals("no reconfiguration expected", 0, configurators.constructed().size());
        }

        assertEquals(1, watchList.changeDetectedCalls);
        assertEquals(Collections.singletonList("l:entered"), notifications);
        statusChecker.assertNoMatch(Pattern.quote(DETECTED_CHANGE_IN_CONFIGURATION_FILES));
        assertContextWasNotReset();
    }

    @Test
    public void changedMainXmlReconfiguresTheResetContextWithoutFallingBack() throws Exception {
        final URL mainXml = xmlUrl("main.xml");
        registerChangedWatchList(mainXml);
        task.addListener(new RecordingListener("l"));

        try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class,
                (mock, ctx) -> doAnswer(invocation -> {
                    JoranConfigurator.informContextOfURLUsedForConfiguration(loggerContext, mainXml);
                    loggerContext.putProperty(NEW_CONFIGURATION_KEY, "applied");
                    return null;
                }).when(mock).doConfigure(mainXml))) {

            task.run();

            assertEquals("no fallback configurator expected", 1, configurators.constructed().size());
            verify(configurators.constructed().get(0)).setContext(loggerContext);
        }

        assertNull("the context should have been reset", loggerContext.getProperty(MARKER_KEY));
        assertNotNull("the Android properties should be set up again after the reset",
                loggerContext.getProperty(CoreConstants.PACKAGE_NAME_KEY));
        assertEquals("applied", loggerContext.getProperty(NEW_CONFIGURATION_KEY));
        assertEquals(mainXml, ConfigurationWatchListUtil.getMainWatchURL(loggerContext));
        statusChecker.assertIsWarningOrErrorFree();
        assertEquals(Arrays.asList("l:entered", "l:changeDetected", "l:done"), notifications);
    }

    @Test
    public void xmlParsingErrorsFallBackToSafeConfigurationWithoutItsIncludes() throws Exception {
        final URL mainXml = xmlUrl("main.xml");
        registerChangedWatchList(mainXml);
        final List<SaxEvent> safeEvents = configurationWithIncludeAndRootLevel(Level.WARN);
        assertEquals(6, safeEvents.size());

        List<SaxEvent> replayedEvents;
        try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class,
                (mock, ctx) -> {
                    if (ctx.getCount() == 1) {
                        // the configurator of the main file: it applies part of the file and reports a parsing error
                        when(mock.recallSafeConfiguration()).thenReturn(safeEvents);
                        doAnswer(invocation -> {
                            JoranConfigurator.informContextOfURLUsedForConfiguration(loggerContext, mainXml);
                            loggerContext.putProperty(NEW_CONFIGURATION_KEY, "partly applied");
                            loggerContext.getStatusManager().add(new ErrorStatus(
                                    CoreConstants.XML_PARSING + " - Parsing fatal error on line 2 and column 9", mock));
                            return null;
                        }).when(mock).doConfigure(mainXml);
                    }
                })) {

            task.run();

            assertEquals(2, configurators.constructed().size());
            JoranConfigurator fallbackConfigurator = configurators.constructed().get(1);
            verify(fallbackConfigurator).setContext(loggerContext);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<SaxEvent>> replayed = ArgumentCaptor.forClass(List.class);
            verify(fallbackConfigurator).doConfigure(replayed.capture());
            replayedEvents = replayed.getValue();
            // the whole safe configuration, includes and all, is registered once more
            verify(fallbackConfigurator).registerSafeConfiguration(safeEvents);
        }

        // only the <include> start and end events are left out of the replay
        List<SaxEvent> expected = new ArrayList<SaxEvent>(safeEvents);
        expected.remove(safeEvents.get(2));
        expected.remove(safeEvents.get(1));
        assertEquals(expected, replayedEvents);

        assertNull("the partly applied configuration should have been reset",
                loggerContext.getProperty(NEW_CONFIGURATION_KEY));
        assertNotNull("the Android properties should be set up again after the second reset",
                loggerContext.getProperty(CoreConstants.PACKAGE_NAME_KEY));
        statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(FALLING_BACK_TO_SAFE_CONFIGURATION));
        statusChecker.assertContainsMatch(Status.INFO, Pattern.quote(RE_REGISTERING_PREVIOUS_SAFE_CONFIGURATION));
        statusChecker.assertContainsMatch(Status.INFO,
                Pattern.quote("after registerSafeConfiguration: " + safeEvents));
        statusChecker.assertNoMatch(Pattern.quote(NO_PREVIOUS_CONFIGURATION));
        assertEquals(mainXml, ConfigurationWatchListUtil.getMainWatchURL(loggerContext));
    }

    @Test
    public void toStringShowsTheBirthdate() {
        task.birthdate = 1234L;

        assertEquals("ReconfigureOnChangeTask(born:1234)", task.toString());
    }

    private URL xmlUrl(String name) throws IOException {
        return new File(tmp.getRoot(), name).toURI().toURL();
    }

    private StubWatchList registerChangedWatchList(URL mainUrl) {
        StubWatchList watchList = new StubWatchList(mainUrl,
                Collections.singletonList(new File(mainUrl.getFile())), true);
        ConfigurationWatchListUtil.registerConfigurationWatchList(loggerContext, watchList);
        return watchList;
    }

    private void assertContextWasNotReset() {
        assertEquals("present", loggerContext.getProperty(MARKER_KEY));
    }

    private Level rootLevel() {
        return loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel();
    }

    private Status findStatus(String message) {
        for (Status s : loggerContext.getStatusManager().getCopyOfStatusList()) {
            if (message.equals(s.getMessage())) {
                return s;
            }
        }
        throw new AssertionError("no status [" + message + "]");
    }

    /**
     * The SAX events of {@code <configuration><root level="..."/></configuration>},
     * recorded through the public ContentHandler API, so no XML parser is needed.
     */
    private List<SaxEvent> configurationWithRootLevel(Level level) {
        SaxEventRecorder recorder = new SaxEventRecorder(loggerContext);
        recorder.setDocumentLocator(new LocatorImpl());
        recorder.startElement("", "configuration", "configuration", new AttributesImpl());
        AttributesImpl rootAttributes = new AttributesImpl();
        rootAttributes.addAttribute("", "level", "level", "CDATA", level.toString());
        recorder.startElement("", "root", "root", rootAttributes);
        recorder.endElement("", "root", "root");
        recorder.endElement("", "configuration", "configuration");
        return recorder.getSaxEventList();
    }

    /**
     * The SAX events of
     * {@code <configuration><include file="included.xml"/><root level="..."/></configuration>}.
     */
    private List<SaxEvent> configurationWithIncludeAndRootLevel(Level level) {
        SaxEventRecorder recorder = new SaxEventRecorder(loggerContext);
        recorder.setDocumentLocator(new LocatorImpl());
        recorder.startElement("", "configuration", "configuration", new AttributesImpl());
        AttributesImpl includeAttributes = new AttributesImpl();
        includeAttributes.addAttribute("", "file", "file", "CDATA", "included.xml");
        recorder.startElement("", "include", "include", includeAttributes);
        recorder.endElement("", "include", "include");
        AttributesImpl rootAttributes = new AttributesImpl();
        rootAttributes.addAttribute("", "level", "level", "CDATA", level.toString());
        recorder.startElement("", "root", "root", rootAttributes);
        recorder.endElement("", "root", "root");
        recorder.endElement("", "configuration", "configuration");
        return recorder.getSaxEventList();
    }

    /** A watch list with a fixed main URL, file list and change status. */
    static class StubWatchList extends ConfigurationWatchList {
        final URL mainUrl;
        final List<File> files;
        final boolean changed;
        int changeDetectedCalls;

        StubWatchList(URL mainUrl, List<File> files, boolean changed) {
            this.mainUrl = mainUrl;
            this.files = files;
            this.changed = changed;
        }

        @Override
        public URL getMainURL() {
            return mainUrl;
        }

        @Override
        public List<File> getCopyOfFileWatchList() {
            return files == null ? null : new ArrayList<File>(files);
        }

        @Override
        public boolean changeDetected() {
            changeDetectedCalls++;
            return changed;
        }
    }

    class RecordingListener extends ReconfigureOnChangeTaskListener {
        final String name;

        RecordingListener(String name) {
            this.name = name;
        }

        @Override
        void enteredRunMethod() {
            notifications.add(name + ":entered");
        }

        @Override
        void changeDetected() {
            notifications.add(name + ":changeDetected");
        }

        @Override
        void doneReconfiguring() {
            notifications.add(name + ":done");
        }
    }
}
