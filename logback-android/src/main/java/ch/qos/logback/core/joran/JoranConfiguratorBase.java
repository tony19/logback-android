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
package ch.qos.logback.core.joran;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.action.ActionConst;
import ch.qos.logback.core.joran.action.AppenderAction;
import ch.qos.logback.core.joran.action.AppenderRefAction;
import ch.qos.logback.core.joran.action.ConversionRuleAction;
import ch.qos.logback.core.joran.action.DefinePropertyAction;
import ch.qos.logback.core.joran.action.NestedBasicPropertyIA;
import ch.qos.logback.core.joran.action.NestedComplexPropertyIA;
import ch.qos.logback.core.joran.action.NewRuleAction;
import ch.qos.logback.core.joran.action.ParamAction;
import ch.qos.logback.core.joran.action.PropertyAction;
import ch.qos.logback.core.joran.action.ShutdownHookAction;
import ch.qos.logback.core.joran.action.StatusListenerAction;
import ch.qos.logback.core.joran.action.TimestampAction;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.RuleStore;

// Based on 310985 revision 310985 as attested by http://tinyurl.com/8njps
// see also http://tinyurl.com/c2rp5

/**
 * A JoranConfiguratorBase lays most of the groundwork for concrete
 * configurators derived from it. Concrete configurators only need to implement
 * the {@link #addInstanceRules} method.
 * <p>
 * A JoranConfiguratorBase instance should not be used more than once to
 * configure a Context.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
abstract public class JoranConfiguratorBase<E> extends GenericConfigurator {

  @Override
  protected void addInstanceRules(RuleStore rs) {

    rs.addRule(new ElementSelector("configuration/property"), new PropertyAction());

    rs.addRule(new ElementSelector("configuration/substitutionProperty"),
        new PropertyAction());

    rs.addRule(new ElementSelector("configuration/timestamp"), new TimestampAction());
    rs.addRule(new ElementSelector("configuration/shutdownHook"), new ShutdownHookAction());
    rs.addRule(new ElementSelector("configuration/define"), new DefinePropertyAction());

    rs.addRule(new ElementSelector("configuration/conversionRule"),
        new ConversionRuleAction());

    rs.addRule(new ElementSelector("configuration/statusListener"),
        new StatusListenerAction());

    rs.addRule(new ElementSelector("configuration/appender"), new AppenderAction<E>());
    rs.addRule(new ElementSelector("configuration/appender/appender-ref"), new AppenderRefAction<E>());
    rs.addRule(new ElementSelector("configuration/newRule"), new NewRuleAction());
    rs.addRule(new ElementSelector("*/param"), new ParamAction());
  }

  @Override
  protected void addImplicitRules(Interpreter interpreter) {
    // The following line adds the capability to parse nested components
    NestedComplexPropertyIA nestedComplexPropertyIA = new NestedComplexPropertyIA();
    nestedComplexPropertyIA.setContext(context);
    interpreter.addImplicitAction(nestedComplexPropertyIA);

    NestedBasicPropertyIA nestedBasicIA = new NestedBasicPropertyIA();
    nestedBasicIA.setContext(context);
    interpreter.addImplicitAction(nestedBasicIA);
  }

  @Override
  protected void buildInterpreter() {
    super.buildInterpreter();
    Map<String, Object> omap = interpreter.getInterpretationContext()
        .getObjectMap();
    omap.put(ActionConst.APPENDER_BAG, new HashMap<String, Appender<?>>());
    //omap.put(ActionConst.FILTER_CHAIN_BAG, new HashMap());
  }

  public InterpretationContext getInterpretationContext() {
    return interpreter.getInterpretationContext();
  }
}
