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
package ch.qos.logback.classic.layout;

import static org.junit.Assert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.CoreConstants;

public class TTLLLayoutTest {

    // TTLL prefix:
    // %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
    final String TTLL_PREFIX_PATTERN = "\\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[[^\\]]+\\] [^\\s]+ " + TTLLLayoutTest.class.getCanonicalName();
    LoggerContext context = new LoggerContext();
    Logger logger = context.getLogger(TTLLLayoutTest.class);
    TTLLLayout layout = new TTLLLayout();

    @Before
    public void setUp() {
        layout.setContext(context);
        layout.start();
    }

    @Test
    public void nullMessage() {
        LoggingEvent event = new LoggingEvent("", logger, Level.INFO, null, null, null);
        event.setTimeStamp(0);
        String result = layout.doLayout(event);
        String firstLine = result.split(CoreConstants.LINE_SEPARATOR)[0];
        assertThat(firstLine, matchesPattern(TTLL_PREFIX_PATTERN + " - null"));
    }
}
