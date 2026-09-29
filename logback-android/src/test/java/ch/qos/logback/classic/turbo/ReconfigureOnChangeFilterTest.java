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
package ch.qos.logback.classic.turbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
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
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class ReconfigureOnChangeFilterTest {

  static final String MARKER_KEY = "rocFilterMarker";
  static final String FALLING_BACK = "Falling back to previously registered safe configuration.";
  static final String RE_REGISTERING = "Re-registering previous fallback configuration once more as a fallback configuration point";
  static final String NO_PREVIOUS_CONFIGURATION = "No previous configuration to fall back on.";
  static final String SAFE_CONFIGURATION_FAILED = "Unexpected exception thrown by a configuration considered safe.";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  ExecutorCapturingLoggerContext loggerContext = new ExecutorCapturingLoggerContext();
  StatusChecker statusChecker = new StatusChecker(loggerContext);
  ReconfigureOnChangeFilter filter = new ReconfigureOnChangeFilter();

  @Before
  public void setUp() {
    loggerContext.setName("rocFilter");
    // reset() clears the context properties: the marker tells whether a reconfiguration took place
    loggerContext.putProperty(MARKER_KEY, "present");
    filter.setContext(loggerContext);
  }

  @Test
  public void startWithoutWatchListWarnsAndLeavesFilterStopped() {
    filter.start();

    assertFalse(filter.isStarted());
    assertNull(filter.mainConfigurationURL);
    statusChecker.assertContainsMatch(Status.WARN, Pattern.quote("Empty ConfigurationWatchList in context"));
  }

  @Test
  public void startWithoutMainUrlWarnsThatReconfigurationIsImpossible() {
    ConfigurationWatchList watchList = new ConfigurationWatchList();
    watchList.setContext(loggerContext);
    ConfigurationWatchListUtil.registerConfigurationWatchList(loggerContext, watchList);

    filter.start();

    assertFalse(filter.isStarted());
    statusChecker.assertContainsMatch(Status.WARN,
        Pattern.quote("Due to missing top level configuration file, automatic reconfiguration is impossible."));
  }

  @Test
  public void startWithMainUrlSchedulesTheFirstCheckOneRefreshPeriodLater() throws IOException {
    File mainFile = tmp.newFile("main.xml");
    URL mainUrl = registerWatchListOn(mainFile);
    filter.setRefreshPeriod(5000);

    long before = System.currentTimeMillis();
    filter.start();
    long after = System.currentTimeMillis();

    assertTrue(filter.isStarted());
    assertEquals(mainUrl, filter.mainConfigurationURL);
    assertTrue("nextCheck=" + filter.nextCheck,
        filter.nextCheck >= before + 5000 && filter.nextCheck <= after + 5000);
    statusChecker.assertContainsMatch(Status.INFO,
        Pattern.quote("Will scan for changes in [[" + mainFile + "]] every 5 seconds. "));
  }

  @Test
  public void refreshPeriodDefaultsToOneMinute() throws IOException {
    assertEquals(60 * 1000L, ReconfigureOnChangeFilter.DEFAULT_REFRESH_PERIOD);
    assertEquals(ReconfigureOnChangeFilter.DEFAULT_REFRESH_PERIOD, filter.getRefreshPeriod());
    File mainFile = tmp.newFile("main.xml");
    registerWatchListOn(mainFile);

    filter.start();

    statusChecker.assertContainsMatch(Status.INFO,
        Pattern.quote("Will scan for changes in [[" + mainFile + "]] every 60 seconds. "));
    filter.setRefreshPeriod(1234);
    assertEquals(1234, filter.getRefreshPeriod());
  }

  @Test
  public void decideIsNeutralAndInertWhenNotStarted() {
    for (int i = 0; i < 64; i++) {
      assertEquals(FilterReply.NEUTRAL, decide());
    }

    assertEquals("ReconfigureOnChangeFilter{invocationCounter=0}", filter.toString());
    verifyNoInteractions(loggerContext.executor);
  }

  @Test
  public void decideLooksForChangesOnlyOnceEverySixteenCalls() throws IOException {
    StubWatchList watchList = startWithStubWatchList(false);
    filter.nextCheck = 0; // a check is due

    for (int i = 0; i < 15; i++) {
      assertEquals(FilterReply.NEUTRAL, decide());
    }
    assertEquals(0, watchList.changeDetectedCalls);

    assertEquals(FilterReply.NEUTRAL, decide());
    assertEquals(1, watchList.changeDetectedCalls);
    assertEquals("ReconfigureOnChangeFilter{invocationCounter=16}", filter.toString());
    verifyNoInteractions(loggerContext.executor);
  }

  @Test
  public void decideDetachesReconfigurationOnlyOnceWhenAChangeIsDetected() throws IOException {
    StubWatchList watchList = startWithStubWatchList(true);
    filter.nextCheck = 0; // a check is due

    for (int i = 0; i < 16; i++) {
      assertEquals(FilterReply.NEUTRAL, decide());
    }

    assertEquals(1, watchList.changeDetectedCalls);
    assertEquals(Long.MAX_VALUE, filter.nextCheck);
    statusChecker.assertContainsMatch(Status.INFO,
        Pattern.quote("Detected change in [" + watchList.getCopyOfFileWatchList() + "]"));
    ArgumentCaptor<Runnable> submitted = ArgumentCaptor.forClass(Runnable.class);
    verify(loggerContext.executor).submit(submitted.capture());
    assertTrue(submitted.getValue() instanceof ReconfigureOnChangeFilter.ReconfiguringThread);

    // subsequent checks never consult the watch list again
    for (int i = 0; i < 256; i++) {
      assertEquals(FilterReply.NEUTRAL, decide());
    }
    assertEquals(1, watchList.changeDetectedCalls);
    verify(loggerContext.executor).submit(any(Runnable.class));
  }

  @Test
  public void maskGrowsWhenChecksComeInQuickSuccession() throws Exception {
    startWithStubWatchList(false);
    setField("mask", 0); // check on every call

    // a last check "in the future" means less than MASK_INCREASE_THRESHOLD ms elapsed
    setField("lastMaskCheck", Long.MAX_VALUE);
    long before = System.currentTimeMillis();
    decide();
    long after = System.currentTimeMillis();
    assertEquals(0x1, getField("mask"));
    long lastMaskCheck = getField("lastMaskCheck");
    assertTrue(lastMaskCheck >= before && lastMaskCheck <= after);

    setField("lastMaskCheck", Long.MAX_VALUE);
    decide();
    assertEquals(0x3, getField("mask"));
  }

  @Test
  public void maskStopsGrowingAtMaxMask() throws Exception {
    startWithStubWatchList(false);
    setField("mask", 0xFFFF);
    setField("invocationCounter", 0xFFFF);
    setField("lastMaskCheck", Long.MAX_VALUE);

    decide();

    assertEquals(0xFFFF, getField("mask"));
    assertTrue(getField("lastMaskCheck") != Long.MAX_VALUE);
  }

  @Test
  public void maskShrinksFourfoldWhenChecksAreFarApart() throws Exception {
    StubWatchList watchList = startWithStubWatchList(false);
    filter.nextCheck = 0;
    // more than MASK_DECREASE_THRESHOLD ms elapsed since the last check
    setField("lastMaskCheck", 0);

    for (int i = 0; i < 16; i++) {
      decide();
    }

    assertEquals(1, watchList.changeDetectedCalls);
    assertEquals(0xF >>> 2, getField("mask"));
  }

  @Test
  public void changeDetectedIsFalseWithoutConsultingTheWatchListBeforeNextCheck() throws IOException {
    StubWatchList watchList = startWithStubWatchList(true);
    filter.nextCheck = 1000;

    assertFalse(filter.changeDetected(999));

    assertEquals(0, watchList.changeDetectedCalls);
    assertEquals(1000, filter.nextCheck);
  }

  @Test
  public void changeDetectedReportsTheWatchListVerdictAndSchedulesTheNextCheckOnceDue() throws IOException {
    StubWatchList watchList = startWithStubWatchList(true);
    filter.setRefreshPeriod(50);
    filter.nextCheck = 1000;

    assertTrue(filter.changeDetected(1000));
    assertEquals(1050, filter.nextCheck);

    watchList.changed = false;
    assertFalse(filter.changeDetected(1060));
    assertEquals(1110, filter.nextCheck);
    assertEquals(2, watchList.changeDetectedCalls);
  }

  @Test
  public void reconfiguringThreadSkipsReconfigurationWithoutMainUrl() {
    filter.new ReconfiguringThread().run();

    statusChecker.assertContainsMatch(Status.INFO,
        Pattern.quote("Due to missing top level configuration file, skipping reconfiguration"));
    statusChecker.assertNoMatch(Pattern.quote(CoreConstants.RESET_MSG_PREFIX));
    assertContextWasNotReset();
  }

  @Test
  public void reconfiguringThreadLeavesContextAloneWhenMainUrlIsNotXml() throws IOException {
    registerWatchListOn(tmp.newFile("logback.properties"));
    filter.start();

    filter.new ReconfiguringThread().run();

    statusChecker.assertContainsMatch(Status.INFO,
        Pattern.quote(CoreConstants.RESET_MSG_PREFIX + "named [rocFilter]"));
    statusChecker.assertIsWarningOrErrorFree();
    assertContextWasNotReset();
  }

  @Test
  public void reconfiguringThreadReconfiguresTheContextFromTheMainXmlFile() throws Exception {
    URL mainUrl = registerWatchListOn(tmp.newFile("main.xml"));
    filter.start();

    try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class)) {
      filter.new ReconfiguringThread().run();

      assertEquals("no fallback configurator expected", 1, configurators.constructed().size());
      JoranConfigurator configurator = configurators.constructed().get(0);
      verify(configurator).setContext(loggerContext);
      verify(configurator).doConfigure(mainUrl);
    }

    assertContextWasReset();
    statusChecker.assertContainsMatch(Status.INFO,
        Pattern.quote(CoreConstants.RESET_MSG_PREFIX + "named [rocFilter]"));
    statusChecker.assertIsWarningOrErrorFree();
  }

  @Test
  public void reconfiguringThreadFallsBackToSafeConfigurationOnXmlParsingErrors() throws Exception {
    final URL mainUrl = registerWatchListOn(tmp.newFile("main.xml"));
    filter.start();
    final List<SaxEvent> safeEvents = configurationWithRootLevel(Level.WARN);

    try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class,
        (mock, ctx) -> {
          if (ctx.getCount() == 1) {
            when(mock.recallSafeConfiguration()).thenReturn(safeEvents);
            doAnswer(invocation -> {
              loggerContext.getStatusManager().add(new ErrorStatus(
                  CoreConstants.XML_PARSING + " - Parsing fatal error on line 2 and column 9", mock));
              return null;
            }).when(mock).doConfigure(mainUrl);
          }
        })) {
      filter.new ReconfiguringThread().run();

      assertEquals(2, configurators.constructed().size());
      JoranConfigurator fallbackConfigurator = configurators.constructed().get(1);
      verify(fallbackConfigurator).setContext(loggerContext);
      verify(fallbackConfigurator).doConfigure(safeEvents);
      verify(fallbackConfigurator).registerSafeConfiguration(safeEvents);
    }

    statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(FALLING_BACK));
    statusChecker.assertContainsMatch(Status.INFO, Pattern.quote(RE_REGISTERING));
    // the context is told about the main URL again, so that it keeps being watched
    assertEquals(mainUrl, ConfigurationWatchListUtil.getMainWatchURL(loggerContext));
  }

  @Test
  public void reconfiguringThreadFallsBackToSafeConfigurationWhenMainXmlCannotBeRead() throws IOException {
    URL missingXml = registerWatchListOn(new File(tmp.getRoot(), "missing.xml"));
    filter.start();
    List<SaxEvent> safeEvents = configurationWithRootLevel(Level.WARN);
    JoranConfigurator safeConfigurator = new JoranConfigurator();
    safeConfigurator.setContext(loggerContext);
    safeConfigurator.registerSafeConfiguration(safeEvents);
    assertEquals(Level.DEBUG, rootLevel());

    filter.new ReconfiguringThread().run();

    assertContextWasReset();
    statusChecker.assertContainsMatch(Status.ERROR, Pattern.quote("Could not open URL [" + missingXml + "]."));
    statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(FALLING_BACK));
    statusChecker.assertContainsMatch(Status.INFO, Pattern.quote(RE_REGISTERING));
    statusChecker.assertNoMatch(Pattern.quote(NO_PREVIOUS_CONFIGURATION));
    // the safe configuration was replayed and registered once more
    assertEquals(Level.WARN, rootLevel());
    assertSame(safeEvents, loggerContext.getObject(CoreConstants.SAFE_JORAN_CONFIGURATION));
    assertEquals(missingXml, ConfigurationWatchListUtil.getMainWatchURL(loggerContext));
  }

  @Test
  public void reconfiguringThreadWarnsWhenThereIsNoSafeConfigurationToFallBackOn() throws IOException {
    URL missingXml = registerWatchListOn(new File(tmp.getRoot(), "missing.xml"));
    filter.start();

    filter.new ReconfiguringThread().run();

    assertContextWasReset();
    statusChecker.assertContainsMatch(Status.ERROR, Pattern.quote("Could not open URL [" + missingXml + "]."));
    statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(NO_PREVIOUS_CONFIGURATION));
    statusChecker.assertNoMatch(Pattern.quote(FALLING_BACK));
  }

  @Test
  public void reconfiguringThreadReportsAFailingSafeConfigurationAsAnError() throws Exception {
    final URL mainUrl = registerWatchListOn(tmp.newFile("main.xml"));
    filter.start();
    final List<SaxEvent> safeEvents = configurationWithRootLevel(Level.WARN);
    final JoranException safeFailure = new JoranException("safe configuration is broken");

    try (MockedConstruction<JoranConfigurator> configurators = mockConstruction(JoranConfigurator.class,
        (mock, ctx) -> {
          if (ctx.getCount() == 1) {
            when(mock.recallSafeConfiguration()).thenReturn(safeEvents);
            doThrow(new JoranException("main configuration is broken")).when(mock).doConfigure(mainUrl);
          } else {
            doThrow(safeFailure).when(mock).doConfigure(anyList());
          }
        })) {
      filter.new ReconfiguringThread().run();

      assertEquals(2, configurators.constructed().size());
      JoranConfigurator fallbackConfigurator = configurators.constructed().get(1);
      verify(fallbackConfigurator).doConfigure(safeEvents);
      verify(fallbackConfigurator, never()).registerSafeConfiguration(any());
    }

    statusChecker.assertContainsMatch(Status.WARN, Pattern.quote(FALLING_BACK));
    Status error = findStatus(SAFE_CONFIGURATION_FAILED);
    assertEquals(Status.ERROR, error.getLevel());
    assertSame(safeFailure, error.getThrowable());
    statusChecker.assertNoMatch(Pattern.quote(RE_REGISTERING));
  }

  private FilterReply decide() {
    return filter.decide(null, null, Level.INFO, "msg", null, null);
  }

  private URL registerWatchListOn(File file) throws IOException {
    URL url = file.toURI().toURL();
    ConfigurationWatchListUtil.setMainWatchURL(loggerContext, url);
    return url;
  }

  private StubWatchList startWithStubWatchList(boolean changed) throws IOException {
    File mainFile = tmp.newFile("main.xml");
    StubWatchList watchList = new StubWatchList(mainFile.toURI().toURL(),
        Collections.singletonList(mainFile), changed);
    ConfigurationWatchListUtil.registerConfigurationWatchList(loggerContext, watchList);
    filter.start();
    assertTrue(filter.isStarted());
    return watchList;
  }

  private long getField(String name) throws Exception {
    return declaredField(name).getLong(filter);
  }

  private void setField(String name, long value) throws Exception {
    declaredField(name).setLong(filter, value);
  }

  private static Field declaredField(String name) throws NoSuchFieldException {
    Field field = ReconfigureOnChangeFilter.class.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  private void assertContextWasNotReset() {
    assertEquals("present", loggerContext.getProperty(MARKER_KEY));
  }

  private void assertContextWasReset() {
    assertNull("the context should have been reset", loggerContext.getProperty(MARKER_KEY));
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

  /** A logger context whose executor only records what is submitted to it. */
  static class ExecutorCapturingLoggerContext extends LoggerContext {
    final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    @Override
    public synchronized ScheduledExecutorService getScheduledExecutorService() {
      return executor;
    }
  }

  /** A watch list with a fixed main URL and file list, and a settable change status. */
  static class StubWatchList extends ConfigurationWatchList {
    final URL mainUrl;
    final List<File> files;
    boolean changed;
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
      return new ArrayList<File>(files);
    }

    @Override
    public boolean changeDetected() {
      changeDetectedCalls++;
      return changed;
    }
  }
}
