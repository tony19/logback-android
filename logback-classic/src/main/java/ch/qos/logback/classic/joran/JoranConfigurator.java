/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
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
import ch.qos.logback.core.joran.spi.Pattern;
import ch.qos.logback.core.joran.spi.RuleStore;

/**
 * This JoranConfiguratorclass adds rules specific to logback-classic.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class JoranConfigurator extends JoranConfiguratorBase {

  @Override
  public void addInstanceRules(RuleStore rs) {
    // parent rules already added
    super.addInstanceRules(rs);

    rs.addRule(new Pattern("configuration"), new ConfigurationAction());

    rs.addRule(new Pattern("configuration/contextName"),
        new ContextNameAction());
    rs.addRule(new Pattern("configuration/contextListener"),
        new LoggerContextListenerAction());

    rs.addRule(new Pattern("configuration/appender/sift"), new SiftAction());
    rs.addRule(new Pattern("configuration/appender/sift/*"), new NOPAction());

    rs.addRule(new Pattern("configuration/logger"), new LoggerAction());
    rs.addRule(new Pattern("configuration/logger/level"), new LevelAction());

    rs.addRule(new Pattern("configuration/root"), new RootLoggerAction());
    rs.addRule(new Pattern("configuration/root/level"), new LevelAction());
    rs.addRule(new Pattern("configuration/logger/appender-ref"),
        new AppenderRefAction());
    rs.addRule(new Pattern("configuration/root/appender-ref"),
        new AppenderRefAction());

    rs.addRule(new Pattern("configuration/include"), new IncludeAction());

    rs.addRule(new Pattern("configuration/consolePlugin"),
        new ConsolePluginAction());

    rs.addRule(new Pattern("configuration/findInclude"), new FindIncludeAction());
    rs.addRule(new Pattern("configuration/findInclude/include"), new ConditionalIncludeAction());
  }

  @Override
  protected void addDefaultNestedComponentRegistryRules(
      DefaultNestedComponentRegistry registry) {
    DefaultNestedComponentRules.addDefaultNestedComponentRegistryRules(registry);
  }

}
