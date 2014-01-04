/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic.net;

import ch.qos.logback.classic.net.testObjectBuilders.MockLoggingEvent;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import com.google.common.base.Strings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Timer;
import org.fest.util.Files;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import static ch.qos.logback.classic.net.testObjectBuilders.SerializedLogFileFactory.addFile;
import static ch.qos.logback.matchers.StatusMatchers.containsMessage;
import static ch.qos.logback.matchers.StatusMatchers.hasLevel;
import static ch.qos.logback.matchers.StatusMatchers.hasNoItemWhichContainsMessage;
import static ch.qos.logback.matchers.StatusMatchers.hasThrowable;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Sebastian Gr&ouml;bler
 */
@RunWith(MockitoJUnitRunner.class)
public class FileBufferingSocketAppenderTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Mock
  private SocketAppender socketAppender;

  @Mock
  private LogFileReader logFileReader;

  @Spy
  private FileBufferingConfiguration configuration = new FileBufferingConfiguration();

  @Mock
  private ObjectIOProvider objectIoProvider;

  @Mock
  private Timer timer;

  @InjectMocks
  private FileBufferingSocketAppender appender;


  private ObjectOutput objectOutput;

  @Before
  public void beforeEachTest() {
    configuration.setLogFolder("/some/folder/");
  }

  @Test
  public void doesNotStartWhenConfigurationIsInvalid() {

    // given
    when(configuration.isInvalid()).thenReturn(Boolean.TRUE);

    // when
    appender.start();

    // then
    assertThat(appender.isStarted(), is(false));
  }

  @Test
  public void addsErrorsWhenConfigurationIsInvalid() {

    // given
    when(configuration.isInvalid()).thenReturn(Boolean.TRUE);

    // when
    appender.start();

    // then
    verify(configuration).addErrors(appender);
  }


  @Test
  public void doesNotStopWhenNotStarted() {
    appender.stop();

    verify(timer, never()).cancel();
    verify(socketAppender, never()).stop();
  }

  @Test
  public void logsErrorOnIOException() throws IOException {

    // given
    final StatusManager statusManager = mockStatusManager();
    final ILoggingEvent event = mock(ILoggingEvent.class);
    when(objectIoProvider.newObjectOutput(anyString())).thenThrow(IOException.class);

    // when
    appender.start();
    appender.append(event);

    // then
    final ArgumentCaptor<Status> captor = ArgumentCaptor.forClass(Status.class);
    verify(statusManager).add(captor.capture());

    final Status status = captor.getValue();
    assertThat(status, containsMessage("Could not write logging event to disk."));
    assertThat(status, hasLevel(Status.ERROR));
    assertThat(status, hasThrowable(IOException.class));
  }

  @Test
  public void startsTimerWithConfiguredIntervalOnStart() {
    appender.start();

    verify(timer).schedule(logFileReader, configuration.getReadInterval(), configuration.getReadInterval());
  }

  @Test
  public void stopsTimerOnStop() {
    // given
    appender.start();

    // when
    appender.stop();

    // then
    verify(timer).cancel();
  }

  @Test
  public void doesNotAppendIfNotStarted() {
    appender.append(mock(ILoggingEvent.class));
    verifyZeroInteractions(objectIoProvider);
  }

  @Test
  public void serializesEventToConfiguredFolder() throws IOException {
    // given
    final String logFolder = folder.getRoot().getAbsolutePath() + "/foo/";
    appender.setLogFolder(logFolder);

    when(objectIoProvider.newObjectOutput(anyString())).then(createObjectOutput());

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    // then
    assertThat(new File(logFolder).list(), hasItemInArray(endsWith(configuration.getFileExtension())));
  }

  @Test
  public void createsMissingDirsWithEveryLoggingEvent() throws IOException {
    // given
    final StatusManager statusManager = mockStatusManager();
    appender.setLogFolder(folder.getRoot().getAbsolutePath() + "/foo");

    when(objectIoProvider.newObjectOutput(anyString())).then(createObjectOutput());

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    Files.delete(new File(configuration.getLogFolder()));

    appender.append(MockLoggingEvent.create());

    // then
    verifyZeroInteractions(statusManager);
  }

  @Test
  public void closesStreamsOnSuccessfulFileWrite() throws IOException {
    // given
    appender.setLogFolder(folder.getRoot().getAbsolutePath() + "/foo/");

    when(objectIoProvider.newObjectOutput(anyString())).then(createObjectOutput());

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    // then
    verify(objectOutput).close();
  }

  @Test
  public void closesStreamsOnUnsuccessfulFileWrite() throws IOException {
    // given
    appender.setLogFolder(folder.getRoot().getAbsolutePath() + "/foo/");

    when(objectIoProvider.newObjectOutput(anyString())).then(createExceptionThrowingObjectOutput());

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    // then
    verify(objectOutput).close();
  }

  @Test
  public void allowsFolderToHaveTrailingSlash() throws IOException {
    // given
    final String logFolder = folder.getRoot().getAbsolutePath() + "/foo/";
    appender.setLogFolder(logFolder);

    when(objectIoProvider.newObjectOutput(anyString())).then(createObjectOutput());

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    // then
    assertThat(new File(logFolder).list(), hasItemInArray(endsWith(configuration.getFileExtension())));
  }

  @Test
  public void allowsFolderToHaveNoTrailingSlash() throws IOException {
    // given
    final String logFolder = folder.getRoot().getAbsolutePath() + "/foo";
    appender.setLogFolder(logFolder);

    when(objectIoProvider.newObjectOutput(anyString())).then(createObjectOutput());

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    // then
    assertThat(new File(logFolder).list(), hasItemInArray(endsWith(configuration.getFileExtension())));
  }

  @Test
  public void loadsCallerDataWhenConfigured() throws IOException {

    // given
    appender.setIncludeCallerData(true);
    objectOutput = mock(ObjectOutput.class);
    when(objectIoProvider.newObjectOutput(anyString())).thenReturn(objectOutput);

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    // then
    verify(socketAppender).postProcessEvent(any(ILoggingEvent.class));
  }

  @Test
  public void doesNotLoadCallerDataWhenConfigured() throws IOException {

    // given
    appender.setIncludeCallerData(false);
    objectOutput = mock(ObjectOutput.class);
    when(objectIoProvider.newObjectOutput(anyString())).thenReturn(objectOutput);

    // when
    appender.start();
    appender.append(MockLoggingEvent.create());

    // then
    final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(objectOutput).writeObject(captor.capture());

    final LoggingEventVO loggingEventVO = (LoggingEventVO) captor.getValue();

    assertThat(loggingEventVO.hasCallerData(), is(false));
  }

  @Test
  public void givesFilesSpecialTemporaryFileEndingWhileWritingToAvoidSimultaneousReadAndWrite() throws IOException {

    // given
    final FileBufferingSocketAppender appender = new FileBufferingSocketAppender();
    final Context context = mock(Context.class);
    final StatusManager statusManager = mock(StatusManager.class);
    when(context.getStatusManager()).thenReturn(statusManager);

    final String logFolder = folder.getRoot().getAbsolutePath() + "/foo/";
    appender.setLogFolder(logFolder);
    appender.setPort(6000);
    appender.setRemoteHost("localhost");
    appender.setContext(context);
    appender.setLazy(true);
    appender.setReadInterval(10);

    // when
    appender.start();
    appender.append(MockLoggingEvent.create().withMessage(Strings.repeat("some random string", 1000000)));
    appender.stop();

    // then
    final ArgumentCaptor<Status> captor = ArgumentCaptor.forClass(Status.class);
    verify(statusManager, atLeast(0)).add(captor.capture());

    assertThat(captor.getAllValues(), hasNoItemWhichContainsMessage("Could not load logging event from disk."));
  }

  @Test
  public void removesOldTempFilesOnStart() throws IOException {
    // given
    final String logFolderPath = folder.getRoot().getAbsolutePath() + "/foo/";
    final File logFolder = new File(logFolderPath);
    logFolder.mkdirs();
    appender.setLogFolder(logFolderPath);

    addFile(logFolder.getAbsolutePath() + "/foo.ser.tmp", DateTime.now());
    addFile(logFolder.getAbsolutePath() + "/bar.ser.tmp", DateTime.now());

    // when
    appender.start();

    // then
    assertThat(logFolder.list(), not(hasItemInArray("foo.ser.tmp")));
    assertThat(logFolder.list(), not(hasItemInArray("bar.ser.tmp")));
  }

  @Test
  public void removesOnlyOldTempFilesButNotDirectoriesOnStart() throws IOException {
    // given
    final String logFolderPath = folder.getRoot().getAbsolutePath() + "/foo/";
    final File logFolder = new File(logFolderPath);
    logFolder.mkdirs();
    appender.setLogFolder(logFolderPath);

    final File tmpFolder = new File(logFolderPath + "/some.tmp");
    tmpFolder.mkdirs();

    // when
    appender.start();

    // then
    assertThat(logFolder.list(), hasItemInArray("some.tmp"));
  }

  @Test
  public void removalOfOldTempFilesOnStartCanHandleNonExistingTempFolder() {
    // given
    appender.setLogFolder(folder.getRoot().getAbsolutePath() + "/foo/");

    // when
    appender.start();
  }

  @Test
  public void delegatesContextToSocketAppender() {

    // given
    final Context context = mock(Context.class);

    // when
    appender.setContext(context);

    // then
    verify(socketAppender).setContext(context);
  }

  @Test
  public void setsNameToSocketAppender() {
    appender.setName("SomeName");
    verify(socketAppender).setName("SomeName-BelongingSocketAppender");
  }

  @Test
  public void startsSocketAppender() {
    appender.start();
    verify(socketAppender).start();
  }

  @Test
  public void stopsSocketAppender() {
    appender.start();
    appender.stop();

    verify(socketAppender).stop();
  }

  @Test
  public void delegatesIncludeCallerDataToSocketAppender() {
    appender.setIncludeCallerData(false);
    verify(socketAppender).setIncludeCallerData(false);
  }

  @Test
  public void delegatesRemoteHostToSocketAppender() {
    appender.setRemoteHost("localhost");
    verify(socketAppender).setRemoteHost("localhost");
  }

  @Test
  public void delegatesPortToSocketAppender() {
    appender.setPort(8081);
    verify(socketAppender).setPort(8081);
  }

  @Test
  public void delegatesReconnectionDelayToSocketAppender() {
    appender.setReconnectionDelay(1000);
    verify(socketAppender).setReconnectionDelay(1000);
  }

  @Test
  public void delegatesLazyToSocketAppender() {
    appender.setLazy(true);
    verify(socketAppender).setLazy(true);
  }

  @Test
  public void delegatesLogFolderToConfiguration() {
    appender.setLogFolder("/foo/bar/");
    verify(configuration).setLogFolder("/foo/bar/");
  }

  @Test
  public void delegatesFileEndingToConfiguration() {
    appender.setFileEnding(".foobar");
    verify(configuration).setFileExtension(".foobar");
  }

  @Test
  public void delegatesBatchSizeToConfiguration() {
    appender.setBatchSize(1000);
    verify(configuration).setBatchSize(1000);
  }

  @Test
  public void delegatesReadIntervalToConfiguration() {
    appender.setReadInterval(5000);
    verify(configuration).setReadInterval(5000);
  }

  @Test
  public void delegatesFileCountQuotaToConfiguration() {
    appender.setFileCountQuota(1000);
    verify(configuration).setFileCountQuota(1000);
  }

  private StatusManager mockStatusManager() {
    final Context context = mock(Context.class);
    final StatusManager statusManager = mock(StatusManager.class);
    appender.setContext(context);
    when(context.getStatusManager()).thenReturn(statusManager);
    return statusManager;
  }

  private Answer<?> createExceptionThrowingObjectOutput() {
    return new Answer<Object>() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        final String fileName = (String) invocation.getArguments()[0];
        objectOutput = spy(new ObjectOutputStream(new FileOutputStream(fileName)));
        doThrow(IOException.class).when(objectOutput).writeObject(any(Object.class));
        return objectOutput;
      }
    };
  }

  private Answer<Object> createObjectOutput() {
    return new Answer<Object>() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        final String fileName = (String) invocation.getArguments()[0];
        objectOutput = spy(new ObjectOutputStream(new FileOutputStream(fileName)));
        return objectOutput;
      }
    };
  }


}
