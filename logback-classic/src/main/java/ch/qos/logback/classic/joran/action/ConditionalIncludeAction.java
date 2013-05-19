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
package ch.qos.logback.classic.joran.action;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.UnknownHostException;

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.AbstractIncludeAction;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * @author Anthony Trinh
 */
public class ConditionalIncludeAction extends AbstractIncludeAction {

  /**
   * Path container, used to determine whether an include has found a path,
   * in which case subsequent includes are ignored
   */
  class State {
    private URL url;
    URL getUrl() { return url; }
    void setUrl(URL url) { this.url = url; }
  }

  @Override
  protected void handleError(String message, Exception e) {
    // treat most errors as warnings
    if (e != null &&
        !(e instanceof FileNotFoundException) &&
        !(e instanceof UnknownHostException)) {
      addWarn(message, e);
    } else {
      addInfo(message);
    }
  }

  @Override
  public void begin(InterpretationContext ic, String name, Attributes attributes)
      throws ActionException {
    // continue processing only if path not found
    if (peekPath(ic) != null) {
      return;
    }
    super.begin(ic, name, attributes);
  }

  @Override
  protected void processInclude(InterpretationContext ic, URL url) throws JoranException {
    pushPath(ic, url);
  }

  private URL peekPath(InterpretationContext ic) {
    if (!ic.isEmpty()) {
      Object topOfStack = ic.peekObject();
      if (topOfStack instanceof State) {
        URL url = ((State)topOfStack).getUrl();
        if (url != null) {
          return url;
        }
      }
    }
    return null;
  }

  private URL pushPath(InterpretationContext ic, URL url) {
    State state = new State();
    state.setUrl(url);
    ic.pushObject(state);
    return url;
  }
}
