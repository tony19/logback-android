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

import ch.qos.logback.core.spi.ContextAware;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Sebastian Gr&ouml;bler
 */
public class FileBufferingConfigurationTest {

  private final FileBufferingConfiguration configuration = new FileBufferingConfiguration();

  @Test
  public void hasDefaultValueForFileExtension() {
    assertThat(configuration.getFileExtension(), is(FileBufferingConfiguration.DEFAULT_FILE_EXT));
  }

  @Test
  public void hasNoDefaultValueForLogFolder() {
    assertThat(configuration.getLogFolder(), is(nullValue()));
  }

  @Test
  public void hasDefaultValueForBatchSize() {
    assertThat(configuration.getBatchSize(), is(FileBufferingConfiguration.DEFAULT_BATCH_SIZE));
  }

  @Test
  public void hasDefaultValueForReadInterval() {
    assertThat(configuration.getReadInterval(), is(FileBufferingConfiguration.DEFAULT_READ_INTERVAL));
  }

  @Test
  public void hasDefaultValueForFileCountQuota() {
    assertThat(configuration.getFileCountQuota(), is(FileBufferingConfiguration.DEFAULT_FILE_COUNT_QUOTA));
  }

  @Test
  public void isValidWithDefaultValues() {

    configuration.setLogFolder("/some/folder/");

    assertThat(configuration.isInvalid(), is(false));
  }

  @Test
  public void doesNotAddErrorForDefaultValues() {

    //given
    configuration.setLogFolder("/some/folder/");
    final ContextAware contextAware = mock(ContextAware.class);

    // when
    configuration.addErrors(contextAware);

    // then
    verifyZeroInteractions(contextAware);
  }

  @Test
  public void addsTrailingSlashToLogFolderIfMissing() {
    configuration.setLogFolder("/some/folder");

    assertThat(configuration.getLogFolder(), is("/some/folder/"));
  }

  @Test
  public void isInvalidWhenLogFolderIsNull() {
    configuration.setLogFolder(null);
    assertThat(configuration.isInvalid(), is(true));
  }

  @Test
  public void isInvalidWhenLogFolderIsEmpty() {
    configuration.setLogFolder("");
    assertThat(configuration.isInvalid(), is(true));
  }

  @Test
  public void isInvalidWhenFileExtensionIsNull() {
    configuration.setFileExtension(null);
    assertThat(configuration.isInvalid(), is(true));
  }

  @Test
  public void isInvalidWhenFileExtensionIsEmpty() {
    configuration.setFileExtension("");
    assertThat(configuration.isInvalid(), is(true));
  }

  @Test
  public void isInvalidWhenBatchSizeIsSmallerThanOne() {
    configuration.setBatchSize(0);
    assertThat(configuration.isInvalid(), is(true));
  }

  @Test
  public void isInvalidWhenReadIntervalIsSmallerThanOne() {
    configuration.setReadInterval(0);
    assertThat(configuration.isInvalid(), is(true));
  }

  @Test
  public void isInvalidWhenFileCountQuotaIsSmallerThanOne() {
    configuration.setFileCountQuota(0);
    assertThat(configuration.isInvalid(), is(true));
  }

  @Test
  public void addsErrorWhenLogFolderIsInvalid() {
    // given
    configuration.setLogFolder(null);
    final ContextAware contextAware = mock(ContextAware.class);

    // when
    configuration.addErrors(contextAware);

    // then
    verify(contextAware).addError("logFolder must not be null nor empty");
  }

  @Test
  public void addsErrorWhenFileExtensionIsInvalid() {
    // given
    configuration.setFileExtension(null);
    final ContextAware contextAware = mock(ContextAware.class);

    // when
    configuration.addErrors(contextAware);

    // then
    verify(contextAware).addError("fileExtension must not be null nor empty");
  }

  @Test
  public void addsErrorWhenBatchSizeIsInvalid() {
    // given
    configuration.setBatchSize(0);
    final ContextAware contextAware = mock(ContextAware.class);

    // when
    configuration.addErrors(contextAware);

    // then
    verify(contextAware).addError("batchSize must be greater than zero");
  }

  @Test
  public void addsErrorWhenReadIntervalIsInvalid() {
    // given
    configuration.setReadInterval(0);
    final ContextAware contextAware = mock(ContextAware.class);

    // when
    configuration.addErrors(contextAware);

    // then
    verify(contextAware).addError("readInterval must be greater than zero");
  }

  @Test
  public void addsErrorWhenFileCountQuotaIsInvalid() {
    // given
    configuration.setFileCountQuota(0);
    final ContextAware contextAware = mock(ContextAware.class);

    // when
    configuration.addErrors(contextAware);

    // then
    verify(contextAware).addError("fileCountQuota must be greater than zero");
  }
}
