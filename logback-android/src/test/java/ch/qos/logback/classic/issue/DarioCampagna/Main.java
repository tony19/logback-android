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
package ch.qos.logback.classic.issue.DarioCampagna;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

import java.util.Locale;

public class Main {
  public static void main(String[] args) throws JoranException {

    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.reset();
    JoranConfigurator joranConfigurator = new JoranConfigurator();
    joranConfigurator.setContext(context);
    joranConfigurator.doConfigure("src/test/java/ch/qos/logback/classic/issue/DarioCampagna/logback-marker.xml");
    IMessageConveyor mc = new MessageConveyor(Locale.getDefault());
    LocLoggerFactory llFactory_default = new LocLoggerFactory(mc);
    LocLogger locLogger = llFactory_default.getLocLogger("defaultLocLogger");
    Marker alwaysMarker = MarkerFactory.getMarker("ALWAYS");
    locLogger.info(alwaysMarker, "This will always appear.");
    locLogger.info("Hello!");
  }
}
