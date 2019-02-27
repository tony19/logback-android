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
package ch.qos.logback.core.sift;

import java.util.List;
import java.util.Map;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Builds new appenders dynamically by running SiftingJoranConfigurator instance,
 * a custom configurator tailored for the contents of the sift element.
 * @param <E>
 */
public abstract class AbstractAppenderFactoryUsingJoran<E> implements AppenderFactory<E> {

  final List<SaxEvent> eventList;
  protected String key;
  protected Map<String, String> parentPropertyMap;

  protected AbstractAppenderFactoryUsingJoran(List<SaxEvent> eventList, String key, Map<String, String> parentPropertyMap) {
    this.eventList = removeSiftElement(eventList);
    this.key = key;
    this.parentPropertyMap = parentPropertyMap;

  }

  List<SaxEvent> removeSiftElement(List<SaxEvent> eventList) {
    return eventList.subList(1, eventList.size() - 1);
  }

  public abstract SiftingJoranConfiguratorBase<E> getSiftingJoranConfigurator(String k);

  public Appender<E> buildAppender(Context context, String discriminatingValue) throws JoranException {
    SiftingJoranConfiguratorBase<E> sjc = getSiftingJoranConfigurator(discriminatingValue);
    sjc.setContext(context);
    sjc.doConfigure(eventList);
    return sjc.getAppender();
  }

  public List<SaxEvent> getEventList() {
    return eventList;
  }

}
