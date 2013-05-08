/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
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
package ch.qos.logback.core.joran;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import static ch.qos.logback.core.CoreConstants.SAFE_JORAN_CONFIGURATION;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.StatusChecker;
import org.xml.sax.InputSource;

import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.Pattern;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.spi.ContextAwareBase;

public abstract class GenericConfigurator extends ContextAwareBase {

  protected Interpreter interpreter;

  /**
   * Configures logback with the configuration XML read from a file,
   * located at the given URL
   *
   * @param url URL to the file, containing the configuration XML
   * @throws JoranException
   */
  public final void doConfigure(URL url) throws JoranException {
    InputStream in = null;
    try {
      informContextOfURLUsedForConfiguration(getContext(), url);
      URLConnection urlConnection = url.openConnection();
      // per http://jira.qos.ch/browse/LBCORE-105
      // per http://jira.qos.ch/browse/LBCORE-127
      urlConnection.setUseCaches(false);

      // this closes the stream for us
      in = urlConnection.getInputStream();
      doConfigure(in);
    } catch (IOException ioe) {
      String errMsg = "Could not open URL [" + url + "].";
      addError(errMsg, ioe);
      throw new JoranException(errMsg, ioe);
    }
  }

  /**
   * Configures logback with the configuration XML read from a file,
   * located at the given path on the host filesystem
   *
   * @param filename path to the file, containing the configuration XML
   * @throws JoranException
   */
  public final void doConfigure(String filename) throws JoranException {
    doConfigure(new File(filename));
  }

  /**
   * Configures logback with the configuration XML read from a given file
   *
   * @param file the file, containing the configuration XML
   * @throws JoranException
   */
  public final void doConfigure(File file) throws JoranException {
    FileInputStream fis = null;
    try {
      informContextOfURLUsedForConfiguration(getContext(), file.toURI().toURL());
      fis = new FileInputStream(file);

      // this closes the stream for us
      doConfigure(fis);
    } catch (IOException ioe) {
      String errMsg = "Could not open [" + file.getPath() + "].";
      addError(errMsg, ioe);
      throw new JoranException(errMsg, ioe);
    }
  }

  /**
   * Adds the URL of the used configuration file to the watch list, which is
   * periodically scanned for changes when the "scan" flag is set in logback.xml
   * ({@code <configuration scan="true" ...>}).
   *
   * @param context the logger context to modify
   * @param url the URL to add
   */
  public static void informContextOfURLUsedForConfiguration(Context context, URL url) {
    ConfigurationWatchListUtil.setMainWatchURL(context, url);
  }

  /**
   * Configures logback with the configuraiton XML read from an input stream,
   * and then closes the stream
   *
   * @param inputStream stream to contents of configuration XML
   * @throws JoranException
   */
  public final void doConfigure(InputStream inputStream) throws JoranException {
    try {
      doConfigure(new InputSource(inputStream));
    } finally {
      try {
        inputStream.close();
      } catch (IOException ioe) {
        String errMsg = "Could not close the stream";
        addError(errMsg, ioe);
        throw new JoranException(errMsg, ioe);
      }
    }
  }

  protected abstract void addInstanceRules(RuleStore rs);

  protected abstract void addImplicitRules(Interpreter interpreter);

  protected void addDefaultNestedComponentRegistryRules(DefaultNestedComponentRegistry registry) {

  }

  /**
   * Gets the initial pattern
   * @return a newly created {@link Pattern}
   */
  protected Pattern initialPattern() {
    return new Pattern();
  }

  /**
   * Builds a generic configuration-XML interpreter
   */
  protected void buildInterpreter() {
    RuleStore rs = new SimpleRuleStore(context);
    addInstanceRules(rs);
    this.interpreter = new Interpreter(context, rs, initialPattern());
    InterpretationContext interpretationContext = interpreter.getInterpretationContext();
    interpretationContext.setContext(context);
    addImplicitRules(interpreter);
    addDefaultNestedComponentRegistryRules(interpretationContext.getDefaultNestedComponentRegistry());
  }

  /**
   * Configures logback with the configuration XML read from an input source.
   *
   * @param inputSource the input source, containing the configuration XML
   * @throws JoranException
   */
  private final void doConfigure(final InputSource inputSource)
          throws JoranException {

    long threshold = System.currentTimeMillis();
    if (!ConfigurationWatchListUtil.wasConfigurationWatchListReset(context)) {
      informContextOfURLUsedForConfiguration(getContext(), null);
    }
    SaxEventRecorder recorder = new SaxEventRecorder();
    recorder.setContext(context);
    recorder.recordEvents(inputSource);
    doConfigure(recorder.getSaxEventList());
    // no exceptions at this level
    StatusChecker statusChecker = new StatusChecker(context);
    if (statusChecker.noXMLParsingErrorsOccurred(threshold)) {
      addInfo("Registering current configuration as safe fallback point");
      registerSafeConfiguration();
    }
  }

  /**
   * Configures logback with SAX events of configuration XML
   *
   * @param eventList list of SAX events
   * @throws JoranException
   */
  public void doConfigure(final List<SaxEvent> eventList)
          throws JoranException {
    buildInterpreter();
    // disallow simultaneous configurations of the same context
    synchronized (context.getConfigurationLock()) {
      interpreter.getEventPlayer().play(eventList);
    }
  }

  /**
   * Register the current event list in the interpreter as a safe
   * configuration point.
   *
   * @since 0.9.30
   */
  public void registerSafeConfiguration() {
    context.putObject(SAFE_JORAN_CONFIGURATION, interpreter.getEventPlayer().getCopyOfPlayerEventList());
  }

  /**
   * Recall the event list previously registered as a safe point.
   */
  public List<SaxEvent> recallSafeConfiguration() {
    return (List<SaxEvent>) context.getObject(SAFE_JORAN_CONFIGURATION);
  }
}
