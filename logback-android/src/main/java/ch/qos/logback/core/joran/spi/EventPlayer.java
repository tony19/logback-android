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
package ch.qos.logback.core.joran.spi;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.joran.event.BodyEvent;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.StartEvent;

public class EventPlayer {

  final Interpreter interpreter;
  List<SaxEvent> eventList;
  int currentIndex;

  public EventPlayer(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  /**
   * Return a copy of the current event list in the player.
   * @return list of SAX events currently in the event player
   * @since 0.9.20
   */
  public List<SaxEvent> getCopyOfPlayerEventList() {
    return new ArrayList<SaxEvent>(eventList);
  }

  public void play(List<SaxEvent> aSaxEventList) {
    eventList = aSaxEventList;
    SaxEvent se;
    for(currentIndex = 0; currentIndex < eventList.size(); currentIndex++) {
      se = eventList.get(currentIndex);

      if(se instanceof StartEvent) {
        interpreter.startElement((StartEvent) se);
        // invoke fireInPlay after startElement processing
        interpreter.getInterpretationContext().fireInPlay(se);
      }
      if(se instanceof BodyEvent) {
        // invoke fireInPlay before  characters processing
        interpreter.getInterpretationContext().fireInPlay(se);
        interpreter.characters((BodyEvent) se);
      }
      if(se instanceof EndEvent) {
        // invoke fireInPlay before endElement processing
        interpreter.getInterpretationContext().fireInPlay(se);
        interpreter.endElement((EndEvent) se);
      }

    }
  }

  public void addEventsDynamically(List<SaxEvent> eventList, int offset) {
    this.eventList.addAll(currentIndex+offset, eventList);
  }
}
