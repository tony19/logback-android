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
package ch.qos.logback.classic.sift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.event.InPlayListener;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;

public class SiftAction extends Action implements InPlayListener {
  List<SaxEvent> seList;

  @Override
  public void begin(InterpretationContext ic, String name, Attributes attributes)
      throws ActionException {
    seList = new ArrayList<SaxEvent>();
    ic.addInPlayListener(this);
  }

  @Override
  public void end(InterpretationContext ic, String name) throws ActionException {
    ic.removeInPlayListener(this);
    Object o = ic.peekObject();
    if (o instanceof SiftingAppender) {
      SiftingAppender sa = (SiftingAppender) o;
      Map<String, String> propertyMap = ic.getCopyOfPropertyMap();
      AppenderFactoryUsingJoran appenderFactory = new AppenderFactoryUsingJoran(seList, sa
          .getDiscriminatorKey(), propertyMap);
      sa.setAppenderFactory(appenderFactory);
    }
  }

  public void inPlay(SaxEvent event) {
    seList.add(event);
  }

  public List<SaxEvent> getSeList() {
    return seList;
  }

}
