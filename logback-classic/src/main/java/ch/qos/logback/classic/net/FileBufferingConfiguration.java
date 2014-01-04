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
import com.google.common.base.Strings;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for {@link FileBufferingSocketAppender}.
 *
 * @author Sebastian Gr&ouml;bler
 */
public class FileBufferingConfiguration {

  public static final String DEFAULT_FILE_EXT = ".ser";
  public static final int DEFAULT_BATCH_SIZE = 50;
  public static final long DEFAULT_READ_INTERVAL = TimeUnit.MINUTES.toMillis(1);
  public static final int DEFAULT_FILE_COUNT_QUOTA = 500;

  private String logFolder;
  private String fileExtension = DEFAULT_FILE_EXT;
  private int batchSize = DEFAULT_BATCH_SIZE;
  private long readInterval = DEFAULT_READ_INTERVAL;
  private int fileCountQuota = DEFAULT_FILE_COUNT_QUOTA;

  /**
   * The path to the folder to save the serialized log events in.
   *
   * @return log folder path
   */
  public String getLogFolder() {
    return logFolder;
  }

  /**
   * Sets the path to the folder to save the serialized log events in.
   * This configuration value is required.
   *
   * @param logFolder log folder path
   */
  public void setLogFolder(final String logFolder) {
    this.logFolder = format(logFolder);
  }

  /**
   * The maximum number of events sent in each batch.
   *
   * @return the max event count
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Sets the maximum number of events sent in each batch.
   * The default value is 50.
   *
   * @param batchSize the max event count
   */
  public void setBatchSize(final int batchSize) {
    this.batchSize = batchSize;
  }

  /**
   * The interval in milliseconds in which events should be sent over the socket
   * or deleted according to the configured quota.
   *
   * @return the read interval
   */
  public long getReadInterval() {
    return readInterval;
  }

  /**
   * Sets the interval in milliseconds in which events should be sent over the socket or
   * deleted according to the configured quota. The default value is one minute.
   *
   * @param readInterval the read interval
   */
  public void setReadInterval(final long readInterval) {
    this.readInterval = readInterval;
  }

  /**
   * The file extension of the serialized log events.
   *
   * @return the file extension
   */
  public String getFileExtension() {
    return fileExtension;
  }

  /**
   * Sets the file extension of the serialized log events. The default value is ".ser".
   *
   * @param fileExtension the file extension
   */
  public void setFileExtension(final String fileExtension) {
    this.fileExtension = fileExtension;
  }

  /**
   * The file count quota, which defines how many files are allowed to be stored on disk.
   *
   * @return the file count quota
   */
  public int getFileCountQuota() {
    return fileCountQuota;
  }

  /**
   * Sets the file count quota, which defines how many files are allowed to be stored on disk.
   * <b>Note:</b> This is not a hard limit, meaning the quota is only maintained in each specified interval.
   * So, serialized log events might grow over this specified quota within the {@code readInterval}.
   *
   * @param fileCountQuota
   */
  public void setFileCountQuota(final int fileCountQuota) {
    this.fileCountQuota = fileCountQuota;
  }

  /**
   * @return true when at least one configuration field is invalid.
   */
  public boolean isInvalid() {
    return isLogFolderInvalid() ||
            isFileExtensionInvalid() ||
            isBatchSizeInvalid() ||
            isReadIntervalInvalid() ||
            isFileCountQuotaInvalid();
  }

  /**
   * Adds error messages to the given {@code contextAware} in case any configuration field is invalid.
   *
   * @param contextAware the context aware implementation to add the error messages to
   */
  public void addErrors(final ContextAware contextAware) {

    if (isLogFolderInvalid()) {
      contextAware.addError("logFolder must not be null nor empty");
    }

    if (isFileExtensionInvalid()) {
      contextAware.addError("fileExtension must not be null nor empty");
    }

    if (isBatchSizeInvalid()) {
      contextAware.addError("batchSize must be greater than zero");
    }

    if (isReadIntervalInvalid()) {
      contextAware.addError("readInterval must be greater than zero");
    }

    if (isFileCountQuotaInvalid()) {
      contextAware.addError("fileCountQuota must be greater than zero");
    }
  }

  /**
   * Makes sure that the given {@code logFolder} always ends with a file separator.
   *
   * @param logFolder the log folder to check
   * @return a log folder path that always ends with a file separator
   */
  private String format(final String logFolder) {

    if (Strings.isNullOrEmpty(logFolder)) {
      return logFolder;
    }

    if (logFolder.endsWith(File.separator)) {
      return logFolder;
    }

    return logFolder + File.separator;
  }

  private boolean isLogFolderInvalid() {
    return Strings.isNullOrEmpty(logFolder);
  }

  private boolean isFileExtensionInvalid() {
    return Strings.isNullOrEmpty(fileExtension);
  }

  private boolean isBatchSizeInvalid() {
    return batchSize < 1;
  }

  private boolean isReadIntervalInvalid() {
    return readInterval < 1;
  }

  private boolean isFileCountQuotaInvalid() {
    return fileCountQuota < 1;
  }
}
