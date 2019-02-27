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
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.GenericConfigurator;
import ch.qos.logback.core.joran.action.*;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.RuleStore;

public abstract class SiftingJoranConfiguratorBase<E> extends
        GenericConfigurator {

  protected final String key;
  protected final String value;
  // properties inherited from the main joran run
  protected final Map<String, String> parentPropertyMap;

  protected SiftingJoranConfiguratorBase(String key, String value, Map<String, String> parentPropertyMap) {
    this.key = key;
    this.value = value;
    this.parentPropertyMap = parentPropertyMap;
  }

  final static String ONE_AND_ONLY_ONE_URL = CoreConstants.CODES_URL
          + "#1andOnly1";

  @Override
  protected void addImplicitRules(Interpreter interpreter) {
    NestedComplexPropertyIA nestedComplexIA = new NestedComplexPropertyIA();
    nestedComplexIA.setContext(context);
    interpreter.addImplicitAction(nestedComplexIA);

    NestedBasicPropertyIA nestedSimpleIA = new NestedBasicPropertyIA();
    nestedSimpleIA.setContext(context);
    interpreter.addImplicitAction(nestedSimpleIA);
  }

  @Override
  protected void addInstanceRules(RuleStore rs) {
    rs.addRule(new ElementSelector("configuration/property"), new PropertyAction());
    rs.addRule(new ElementSelector("configuration/timestamp"), new TimestampAction());
    rs.addRule(new ElementSelector("configuration/define"), new DefinePropertyAction());
  }

  abstract public Appender<E> getAppender();

  int errorEmmissionCount = 0;

  protected void oneAndOnlyOneCheck(Map<?, ?> appenderMap) {
    String errMsg = null;
    if (appenderMap.size() == 0) {
      errorEmmissionCount++;
      errMsg = "No nested appenders found within the <sift> element in SiftingAppender.";
    } else if (appenderMap.size() > 1) {
      errorEmmissionCount++;
      errMsg = "Only and only one appender can be nested the <sift> element in SiftingAppender. See also "
              + ONE_AND_ONLY_ONE_URL;
    }

    if (errMsg != null && errorEmmissionCount < CoreConstants.MAX_ERROR_COUNT) {
      addError(errMsg);
    }
  }

  public void doConfigure(final List<SaxEvent> eventList) throws JoranException {
    super.doConfigure(eventList);
  }

  @Override
  public String toString() {
    return this.getClass().getName() + "{" + key + "=" + value + '}';
  }
}
