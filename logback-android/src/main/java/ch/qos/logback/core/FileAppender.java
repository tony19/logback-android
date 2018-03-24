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
package ch.qos.logback.core;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import ch.qos.logback.core.recovery.ResilientFileOutputStream;
import ch.qos.logback.core.util.EnvUtil;
import ch.qos.logback.core.util.FileUtil;

/**
 * FileAppender appends log events to a file.
 *
 * For more information about this appender, please refer to the online manual
 * at http://logback.qos.ch/manual/appenders.html#FileAppender
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class FileAppender<E> extends OutputStreamAppender<E> {

  /**
   * Append to or truncate the file? The default value for this variable is
   * <code>true</code>, meaning that by default a <code>FileAppender</code> will
   * append to an existing file and not truncate it.
   */
  protected boolean append = true;

  /**
   * The name of the active log file.
   */
  protected String fileName = null;

  private boolean prudent = false;
  private boolean initialized = false;
  private boolean lazyInit = false;

  /**
   * The <b>File</b> property takes a string value which should be the name of
   * the file to append to.
   * @param file path to destination log file
   */
  public void setFile(String file) {
    if (file == null) {
      fileName = null;
    } else {
      // Trim spaces from both ends. The users probably does not want
      // trailing spaces in file names.
      fileName = file.trim();
    }
  }

  /**
   * Returns the value of the <b>Append</b> property.
   * @return true if file should be appended to instead of overwritten
   */
  public boolean isAppend() {
    return append;
  }

  /**
   * This method is used by derived classes to obtain the raw file property.
   * Regular users should not be calling this method. Note that RollingFilePolicyBase
   * requires public getter for this property.
   *
   * @return the value of the file property
   */
  final public String rawFileProperty() {
    return fileName;
  }

  /**
   * Returns the value of the <b>File</b> property.
   * @return the path to the destination log file
   */
  public String getFile() {
    return fileName;
  }

  /**
   * If the value of <b>File</b> is not <code>null</code>, then
   * {@link #openFile} is called with the values of <b>File</b> and
   * <b>Append</b> properties.
   */
  public void start() {
    int errors = 0;

    // Use getFile() instead of direct access to fileName because
    // the function is overridden in RollingFileAppender, which
    // returns a value that doesn't necessarily match fileName.
    String file = getFile();

    if (file != null) {
      file = getAbsoluteFilePath(file);
      addInfo("File property is set to [" + file + "]");

      if (prudent) {
        if (!isAppend()) {
          setAppend(true);
          addWarn("Setting \"Append\" property to true on account of \"Prudent\" mode");
        }
      }

      if (!lazyInit) {
        try {
          openFile(file);
        } catch (IOException e) {
          errors++;
          addError("openFile(" + file + "," + append + ") failed", e);
        }
      } else {
        // We'll initialize the file output stream later. Use a dummy for now
        // to satisfy OutputStreamAppender.start().
        setOutputStream(new NOPOutputStream());
      }
    } else {
      errors++;
      addError("\"File\" property not set for appender named [" + name + "]");
    }
    if (errors == 0) {
      super.start();
    }
  }

  /**
   * <p>
   * Sets and <i>opens</i> the file where the log output will go. The specified
   * file must be writable.
   *
   * <p>
   * If there was already an opened file, then the previous file is closed
   * first.
   *
   * <p>
   * <b>Do not use this method directly. To configure a FileAppender or one of
   * its subclasses, set its properties one by one and then call start().</b>
   *
   * @param filename
   *          The path to the log file.
   *
   * @return true if successful; false otherwise
   * @throws IOException file could not be opened
   */
  protected boolean openFile(String filename) throws IOException {
    boolean successful = false;
    filename = getAbsoluteFilePath(filename);
    lock.lock();
    try {
      File file = new File(filename);
      if (FileUtil.isParentDirectoryCreationRequired(file)) {
        boolean result = FileUtil.createMissingParentDirectories(file);
        if (!result) {
          addError("Failed to create parent directories for ["
              + file.getAbsolutePath() + "]");
        }
      }

      ResilientFileOutputStream resilientFos = new ResilientFileOutputStream(
          file, append);
      resilientFos.setContext(context);
      setOutputStream(resilientFos);
      successful = true;
    } finally {
      lock.unlock();
    }
    return successful;
  }

  /**
   * @see #setPrudent(boolean)
   *
   * @return true if in prudent mode
   */
  public boolean isPrudent() {
    return prudent;
  }

  /**
   * When prudent is set to true, file appenders from multiple JVMs can safely
   * write to the same file.
   *
   * @param prudent whether to enable prudent mode
   */
  public void setPrudent(boolean prudent) {
    this.prudent = prudent;
  }

  public void setAppend(boolean append) {
    this.append = append;
  }

  /**
   * Gets the enable status of lazy initialization of the file output
   * stream
   *
   * @return true if enabled; false otherwise
   */
  public boolean getLazy() {
    return lazyInit;
  }

  /**
   * Enables/disables lazy initialization of the file output stream.
   * This defers the file creation until the first outgoing message.
   *
   * @param enable true to enable lazy initialization; false otherwise
   */
  public void setLazy(boolean enable) {
    lazyInit = enable;
  }

  private void safeWrite(E event) throws IOException {
    ResilientFileOutputStream resilientFOS = (ResilientFileOutputStream) getOutputStream();
    FileChannel fileChannel = resilientFOS.getChannel();
    if (fileChannel == null) {
      return;
    }
    FileLock fileLock = null;
    try {
      fileLock = fileChannel.lock();
      long position = fileChannel.position();
      long size = fileChannel.size();
      if (size != position) {
        fileChannel.position(size);
      }
      super.writeOut(event);
    } finally {
      if (fileLock != null) {
        fileLock.release();
      }
    }
  }

  @Override
  protected void writeOut(E event) throws IOException {
    if (prudent) {
      safeWrite(event);
    } else {
      super.writeOut(event);
    }
  }

  @Override
  protected void subAppend(E event) {
    if (!initialized && lazyInit) {
      initialized = true;
      try {
        openFile(getFile());
      } catch (IOException e) {
        this.started = false;
        addError("openFile(" + fileName + "," + append + ") failed", e);
      }
    }

    super.subAppend(event);
  }

  /**
   * Gets the absolute path to the filename, starting from the app's
   * "files" directory, if it is not already an absolute path
   *
   * @param filename filename to evaluate
   * @return absolute path to the filename
   */
  private String getAbsoluteFilePath(String filename) {
    // In Android, relative paths created with File() are relative
    // to root, so fix it by prefixing the path to the app's "files"
    // directory.
    // This transformation is rather expensive, since it involves loading the
    // Android manifest from the APK (which is a ZIP file), and parsing it to
    // retrieve the application package name. This should be avoided if
    // possible as it may perceptibly delay the app launch time.
    if (EnvUtil.isAndroidOS() && !new File(filename).isAbsolute()) {
      String dataDir = context.getProperty(CoreConstants.DATA_DIR_KEY);
      filename = FileUtil.prefixRelativePath(dataDir, filename);
    }
    return filename;
  }
}
