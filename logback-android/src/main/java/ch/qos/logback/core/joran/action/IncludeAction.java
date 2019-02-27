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
package ch.qos.logback.core.joran.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;

public class IncludeAction extends AbstractIncludeAction {

  private static final String INCLUDED_TAG = "included";
  private static final String CONFIG_TAG = "configuration";
  private int eventOffset = 2;

  /**
   * Sets the list index into the event player's list, where the events
   * from the processed include will be inserted
   * @param offset the array index
   */
  protected void setEventOffset(int offset) {
    this.eventOffset = offset;
  }

  /**
   * Creates a SAX event recorder based on given parameters. Subclasses
   * should override this as necessary.
   * @param in input stream
   * @param url URL to opened file/resource
   * @return the newly created recorder
   */
  protected SaxEventRecorder createRecorder(InputStream in, URL url) {
    return new SaxEventRecorder(getContext());
  }

  /**
   * Processes an include
   * @param ic context
   * @param url URL to file/resource being included
   */
  @Override
  protected void processInclude(InterpretationContext ic, URL url) throws JoranException {

    InputStream in = openURL(url);

    try {
      if (in != null) {
        // add URL to watch list in case the "scan" flag is true, in
        // which case this URL is periodically checked for changes
        ConfigurationWatchListUtil.addToWatchList(getContext(), url);

        // parse the include
        SaxEventRecorder recorder = createRecorder(in, url);
        recorder.setContext(getContext());
        recorder.recordEvents(in);

        // remove the leading/trailing tags (<included> or <configuration>)
        trimHeadAndTail(recorder);

        ic.getJoranInterpreter().getEventPlayer().addEventsDynamically(recorder.getSaxEventList(), this.eventOffset);
      }
    } catch (JoranException e) {
      optionalWarning("Failed processing [" + url.toString() + "]", e);
    } finally {
      close(in);
    }
  }

  /**
   * Opens the given URL, logging any exceptions
   * @param url URL of file/resource to open
   * @return an input stream to the URL; or {@code null} if the URL could not be opened
   */
  private InputStream openURL(URL url) {
    try {
      return url.openStream();
    } catch (IOException e) {
      optionalWarning("Failed to open [" + url.toString() + "]", e);
      return null;
    }
  }

  /**
   * Removes the head tag and tail tag if they are named either
   * "included" or "configuration"
   * @param recorder the SAX Event recorder containing the tags
   */
  private void trimHeadAndTail(SaxEventRecorder recorder) {
    List<SaxEvent> saxEventList = recorder.getSaxEventList();
    if (saxEventList.size() == 0) {
      return;
    }

    boolean includedTagFound = false;
    boolean configTagFound = false;

    // find opening element
    SaxEvent first = saxEventList.get(0);
    if (first != null) {
      String elemName = getEventName(first);
      includedTagFound = INCLUDED_TAG.equalsIgnoreCase(elemName);
      configTagFound = CONFIG_TAG.equalsIgnoreCase(elemName);
    }

    // if opening element found, remove it, and then remove the closing element
    if (includedTagFound || configTagFound) {
      saxEventList.remove(0);

      final int listSize = saxEventList.size();
      if (listSize == 0) {
        return;
      }

      final int lastIndex = listSize - 1;
      SaxEvent last = saxEventList.get(lastIndex);

      if (last != null) {
        String elemName = getEventName(last);
        if ((includedTagFound && INCLUDED_TAG.equalsIgnoreCase(elemName)) ||
          (configTagFound && CONFIG_TAG.equalsIgnoreCase(elemName))) {

          saxEventList.remove(lastIndex);
        }
      }
    }
  }

  /**
   * Gets the event name of a {@code SaxEvent}
   * @param event SaxEvent to evaluate
   * @return {@code event.qName} is if it's not empty; otherwise, {@code event.localName}
   */
  private String getEventName(SaxEvent event) {
    return event.qName.length() > 0 ? event.qName : event.localName;
  }
}
