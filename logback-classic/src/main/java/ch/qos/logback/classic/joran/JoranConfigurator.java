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
package ch.qos.logback.classic.joran;

import ch.qos.logback.classic.joran.action.*;
import ch.qos.logback.classic.sift.SiftAction;
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
public class JoranConfigurator extends JoranConfiguratorBase {

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
        new AppenderRefAction());
    rs.addRule(new ElementSelector("configuration/root/appender-ref"),
        new AppenderRefAction());

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
