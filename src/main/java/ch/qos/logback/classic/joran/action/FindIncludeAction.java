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

import java.io.InputStream;
import java.net.URL;

import org.xml.sax.Attributes;

import ch.qos.logback.classic.android.ASaxEventRecorder;
import ch.qos.logback.core.joran.action.IncludeAction;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Action that searches child includes until the first found
 * path is returned.
 *
 * @author Anthony Trinh
 */
public class FindIncludeAction extends IncludeAction {

  private static final int EVENT_OFFSET = 1;

  public FindIncludeAction() {
    setEventOffset(EVENT_OFFSET);
  }

  @Override
  public void begin(InterpretationContext ec, String name, Attributes attributes)
          throws ActionException {
    // nothing to do
  }

  @Override
  public void end(InterpretationContext ic, String name) throws ActionException {
    if (!ic.isEmpty() && (ic.peekObject() instanceof ConditionalIncludeAction.State)) {
      ConditionalIncludeAction.State state = (ConditionalIncludeAction.State)ic.popObject();
      URL url = state.getUrl();
      if (url != null) {
        addInfo("Path found [" + url.toString() + "]");

        try {
          processInclude(ic, url);
        } catch (JoranException e) {
          addError("Failed to process include [" + url.toString() + "]", e);
        }
      } else {
        addInfo("No paths found from includes");
      }
    }
  }

  /**
   * Creates a {@link SaxEventRecorder} based on the input stream
   * @return the newly created recorder
   */
  @Override
  protected SaxEventRecorder createRecorder(InputStream in, URL url) {
    SaxEventRecorder recorder;

    // if the stream URL is named AndroidManifest.xml, assume it's
    // the manifest in binary XML
    if (url.toString().endsWith("AndroidManifest.xml")) {
      // create an AXml parser that only takes XML inside a logback element
      ASaxEventRecorder rec = new ASaxEventRecorder();
      rec.setFilter("logback");
      recorder = rec;
    } else {
      recorder = new SaxEventRecorder(getContext());
    }
    return recorder;
  }
}
