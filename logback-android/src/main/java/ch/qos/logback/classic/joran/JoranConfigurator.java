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
package ch.qos.logback.classic.joran;

import ch.qos.logback.classic.joran.action.*;
import ch.qos.logback.classic.sift.SiftAction;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.DefaultNestedComponentRules;
import ch.qos.logback.classic.joran.action.ConditionalIncludeAction;
import ch.qos.logback.classic.joran.action.FindIncludeAction;
import ch.qos.logback.core.joran.JoranConfiguratorBase;
import ch.qos.logback.core.joran.action.AppenderRefAction;
import ch.qos.logback.core.joran.action.IncludeAction;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;

/**
 * JoranConfigurator class adds rules specific to logback-classic.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class JoranConfigurator extends JoranConfiguratorBase<ILoggingEvent> {

  @SuppressWarnings("deprecation")
  @Override
  public void addInstanceRules(RuleStore rs) {
    // parent rules already added
    super.addInstanceRules(rs);

    rs.addRule(new ElementSelector("configuration"), new ConfigurationAction());

    rs.addRule(new ElementSelector("configuration/contextName"),
        new ContextNameAction());
      rs.addRule(new ElementSelector("configuration/contextListener"),
        new LoggerContextListenerAction());

    rs.addRule(new ElementSelector("configuration/appender/sift"), new SiftAction());
    rs.addRule(new ElementSelector("configuration/appender/sift/*"), new NOPAction());

    rs.addRule(new ElementSelector("configuration/logger"), new LoggerAction());
    rs.addRule(new ElementSelector("configuration/logger/level"), new LevelAction());

    rs.addRule(new ElementSelector("configuration/root"), new RootLoggerAction());
    rs.addRule(new ElementSelector("configuration/root/level"), new LevelAction());
    rs.addRule(new ElementSelector("configuration/logger/appender-ref"),
        new AppenderRefAction<ILoggingEvent>());
    rs.addRule(new ElementSelector("configuration/root/appender-ref"),
        new AppenderRefAction<ILoggingEvent>());

    rs.addRule(new ElementSelector("configuration/include"), new IncludeAction());

    rs.addRule(new ElementSelector("configuration/includes"), new FindIncludeAction());
    rs.addRule(new ElementSelector("configuration/includes/include"), new ConditionalIncludeAction());

    rs.addRule(new ElementSelector("configuration/receiver"),
        new ReceiverAction());
  }

  @Override
  protected void addDefaultNestedComponentRegistryRules(
      DefaultNestedComponentRegistry registry) {
    DefaultNestedComponentRules.addDefaultNestedComponentRegistryRules(registry);
  }

}
