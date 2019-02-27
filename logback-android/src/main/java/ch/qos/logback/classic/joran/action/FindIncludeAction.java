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
package ch.qos.logback.classic.joran.action;

import java.io.InputStream;
import java.net.URL;

import org.xml.sax.Attributes;

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
    return new SaxEventRecorder(getContext());
  }
}
