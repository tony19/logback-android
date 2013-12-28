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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
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
import static ch.qos.logback.matchers.LoggingEventMatchers.containsMessage;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * @author Sebastian Gr&ouml;bler
 */
@RunWith(MockitoJUnitRunner.class)
public class LogFileReaderTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Mock
  private SocketAppender appender;

  @Spy
  private FileBufferingConfiguration configuration = new FileBufferingConfiguration();

  @Mock
  private ObjectIOProvider objectIoProvider;

  @InjectMocks
  private LogFileReader logFileReader;

  private File logFolder;

  @Before
  public void beforeEachTest() throws IOException {
    final String logFolderPath = folder.getRoot().getAbsolutePath() + "/foo/";
    logFolder = new File(logFolderPath);
    logFolder.mkdirs();

    when(configuration.getLogFolder()).thenReturn(logFolderPath);
    when(appender.tryAppend(any(ILoggingEvent.class))).thenReturn(Boolean.TRUE);
  }

  @Test
  public void deletesOldestEventsWhichAreOverTheQuota() throws IOException {

    // given
    when(objectIoProvider.newObjectInput(any(File.class))).thenAnswer(newObjectInput());
    when(configuration.getFileCountQuota()).thenReturn(3);
    when(configuration.getBatchSize()).thenReturn(0);
    addFile(toPath("a.ser"), DateTime.now().plusMinutes(1));
    addFile(toPath("b.ser"), DateTime.now().plusMinutes(2));
    addFile(toPath("c.ser"), DateTime.now().plusMinutes(3));
    addFile(toPath("d.ser"), DateTime.now().plusMinutes(4));
    addFile(toPath("e.ser"), DateTime.now().plusMinutes(5));

    // when
    logFileReader.run();

    // then
    assertThat(logFolder.list(), not(hasItemInArray("a.ser")));
    assertThat(logFolder.list(), not(hasItemInArray("b.ser")));
    assertThat(logFolder.list(), hasItemInArray("c.ser"));
    assertThat(logFolder.list(), hasItemInArray("d.ser"));
    assertThat(logFolder.list(), hasItemInArray("e.ser"));
  }

  @Test
  public void sendsOldestEventsFirst() throws IOException {
    // given
    when(objectIoProvider.newObjectInput(any(File.class))).thenAnswer(newObjectInput());
    addFile(MockLoggingEvent.create().withMessage("a"), toPath("a.ser"), DateTime.now().plusMinutes(1));
    addFile(MockLoggingEvent.create().withMessage("b"), toPath("b.ser"), DateTime.now().plusMinutes(2));
    addFile(MockLoggingEvent.create().withMessage("c"), toPath("c.ser"), DateTime.now().plusMinutes(3));
    addFile(MockLoggingEvent.create().withMessage("d"), toPath("d.ser"), DateTime.now().plusMinutes(4));
    addFile(MockLoggingEvent.create().withMessage("e"), toPath("e.ser"), DateTime.now().plusMinutes(5));

    // when
    logFileReader.run();

    // then
    final ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
    verify(appender, times(5)).tryAppend(captor.capture());

    assertThat(captor.getAllValues().get(0), containsMessage("a"));
    assertThat(captor.getAllValues().get(1), containsMessage("b"));
    assertThat(captor.getAllValues().get(2), containsMessage("c"));
    assertThat(captor.getAllValues().get(3), containsMessage("d"));
    assertThat(captor.getAllValues().get(4), containsMessage("e"));
  }

  @Test
  public void onlySendsTheConfiguredBatchSize() throws IOException {
    // given
    when(objectIoProvider.newObjectInput(any(File.class))).thenAnswer(newObjectInput());
    when(configuration.getBatchSize()).thenReturn(3);
    addFile(MockLoggingEvent.create().withMessage("a"), toPath("a.ser"), DateTime.now().plusMinutes(1));
    addFile(MockLoggingEvent.create().withMessage("b"), toPath("b.ser"), DateTime.now().plusMinutes(2));
    addFile(MockLoggingEvent.create().withMessage("c"), toPath("c.ser"), DateTime.now().plusMinutes(3));
    addFile(MockLoggingEvent.create().withMessage("d"), toPath("d.ser"), DateTime.now().plusMinutes(4));
    addFile(MockLoggingEvent.create().withMessage("e"), toPath("e.ser"), DateTime.now().plusMinutes(5));

    // when
    logFileReader.run();

    // then
    final ArgumentCaptor<ILoggingEvent> captor = ArgumentCaptor.forClass(ILoggingEvent.class);
    verify(appender, times(3)).tryAppend(captor.capture());

    assertThat(captor.getAllValues().get(0), containsMessage("a"));
    assertThat(captor.getAllValues().get(1), containsMessage("b"));
    assertThat(captor.getAllValues().get(2), containsMessage("c"));
  }

  @Test
  public void doesNotSendEventsWhenAppenderIsConnecting() throws IOException {
    // given
    when(objectIoProvider.newObjectInput(any(File.class))).thenAnswer(newObjectInput());
    when(appender.isConnecting()).thenReturn(Boolean.TRUE);
    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    verify(appender, times(0)).tryAppend(any(ILoggingEvent.class));
  }

  @Test
  public void deletesEventWhichCouldNotBeDeserialized() throws IOException, ClassNotFoundException {
    // given
    final ObjectInput objectInput = mock(ObjectInput.class);
    when(objectIoProvider.newObjectInput(any(File.class))).thenReturn(objectInput);
    when(objectInput.readObject()).thenThrow(IOException.class);

    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    assertThat(logFolder.list(), arrayWithSize(0));
  }

  @Test
  public void onlyDeletesEventsWhenNoIOExceptionOccurredDuringTransmission() throws IOException {
    // given
    when(objectIoProvider.newObjectInput(any(File.class))).thenAnswer(newObjectInput());
    when(appender.tryAppend(any(ILoggingEvent.class))).thenReturn(Boolean.FALSE);
    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    assertThat(logFolder.list(), hasItemInArray("a.ser"));
  }

  @Test
  public void logsWarningWhenEventCouldNotBeDeserialized() throws IOException, ClassNotFoundException {
    // given
    final ObjectInput objectInput = mock(ObjectInput.class);
    when(objectIoProvider.newObjectInput(any(File.class))).thenReturn(objectInput);
    when(objectInput.readObject()).thenThrow(IOException.class);

    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    verify(appender).addWarn(matches("^Deserialization for logging event at (\\S+)/foo/a.ser failed. Deleting file.$"));
  }

  @Test
  public void logsErrorWhenEventFileCouldNotBeFound() throws IOException, ClassNotFoundException {
    // given
    final ObjectInput objectInput = mock(ObjectInput.class);
    when(objectIoProvider.newObjectInput(any(File.class))).thenReturn(objectInput);
    when(objectInput.readObject()).thenThrow(FileNotFoundException.class);

    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    verify(appender).addError(eq("Could not find logging event on disk."), any(FileNotFoundException.class));
  }

  @Test
  public void logsErrorWhenILoggingEventCouldNotBeFound() throws IOException, ClassNotFoundException {
    final ObjectInput objectInput = mock(ObjectInput.class);
    when(objectIoProvider.newObjectInput(any(File.class))).thenReturn(objectInput);
    when(objectInput.readObject()).thenThrow(ClassNotFoundException.class);

    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    verify(appender).addError(eq("Could not de-serialize logging event from disk."), any(ClassNotFoundException.class));
  }

  @Test
  public void logsErrorsWhenDeserializationFailedDueToIOException() throws IOException, ClassNotFoundException {
    final ObjectInput objectInput = mock(ObjectInput.class);
    when(objectIoProvider.newObjectInput(any(File.class))).thenReturn(objectInput);
    when(objectInput.readObject()).thenThrow(IOException.class);

    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    verify(appender).addError(eq("Could not load logging event from disk."), any(IOException.class));
  }

  @Test
  public void closesStreamOnSuccessfulFileRead() throws IOException, ClassNotFoundException {

    // given
    final ObjectInput objectInput = mock(ObjectInput.class);
    when(objectInput.readObject()).thenReturn(mock(ILoggingEvent.class));
    when(objectIoProvider.newObjectInput(any(File.class))).thenReturn(objectInput);

    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    verify(objectInput).close();
  }

  @Test
  public void closesStreamOnUnsuccessfulFileRead() throws IOException, ClassNotFoundException {
    final ObjectInput objectInput = mock(ObjectInput.class);
    when(objectIoProvider.newObjectInput(any(File.class))).thenReturn(objectInput);
    when(objectInput.readObject()).thenThrow(IOException.class);

    addFile(toPath("a.ser"), DateTime.now());

    // when
    logFileReader.run();

    // then
    verify(objectInput).close();
  }

  @Test
  public void onlyReadsFilesWithTheConfiguredFileEnding() throws IOException {
    // given
    when(objectIoProvider.newObjectInput(any(File.class))).thenAnswer(newObjectInput());
    addFile(toPath("a.ser"), DateTime.now().plusMinutes(1));
    addFile(toPath("foo.bar"), DateTime.now().plusMinutes(2));

    // when
    logFileReader.run();

    // then
    assertThat(logFolder.list(), hasItemInArray("foo.bar"));
  }

  @Test
  public void canHandleEmptyLogFolder() throws IOException {
    // when
    logFileReader.run();

    // then
    verifyZeroInteractions(objectIoProvider);
    verify(appender, never()).tryAppend(any(ILoggingEvent.class));
  }

  @Test
  public void canHandleNonExistingLogFolder() throws IOException {
    // given
    when(configuration.getLogFolder()).thenReturn("noLogFolderName");

    // when
    logFileReader.run();

    // then
    verifyZeroInteractions(objectIoProvider);
    verify(appender, never()).tryAppend(any(ILoggingEvent.class));
  }

  private String toPath(final String fileName) {
    return logFolder.getAbsolutePath() + "/" + fileName;
  }

  private Answer<?> newObjectInput() {
    return new Answer<Object>() {
      @Override
      public Object answer(final InvocationOnMock invocation) throws Throwable {
        final File file = (File) invocation.getArguments()[0];
        final ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
        return objectInputStream;
      }
    };
  }
}
