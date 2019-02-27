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

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.util.CachingDateFormatter;

/**
 * A layout with a fixed format. The output is equivalent to that produced by {@link ch.qos.logback.classic.PatternLayout PatternLayout} with the pattern:</p>
 *
 * <pre>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pre>
 *
 *<p>TTLLLayout has the advantage of faster load time whereas {@link ch.qos.logback.classic.PatternLayout PatternLayout}
 * requires roughly 40 milliseconds to load its parser classes.  Note that the second run of PatternLayout will be much much faster (approx. 10 micro-seconds).</p>
 *
 * <p>Fixed format layouts such as TTLLLayout should be considered as an alternative to PatternLayout only if the extra 40 milliseconds at application start-up is considered significant.</p>
 *
 * @author Ceki G&uuml;lc&uuml;
 * @since 1.1.6
 */
public class TTLLLayout extends LayoutBase<ILoggingEvent> {

    CachingDateFormatter cachingDateFormatter = new CachingDateFormatter("HH:mm:ss.SSS");
    ThrowableProxyConverter tpc = new ThrowableProxyConverter();

    @Override
    public void start() {
        tpc.start();
        super.start();
    }

    public String doLayout(ILoggingEvent event) {
        if (!isStarted()) {
            return CoreConstants.EMPTY_STRING;
        }
        StringBuilder sb = new StringBuilder();

        long timestamp = event.getTimeStamp();

        sb.append(cachingDateFormatter.format(timestamp));
        sb.append(" [");
        sb.append(event.getThreadName());
        sb.append("] ");
        sb.append(event.getLevel().toString());
        sb.append(" ");
        sb.append(event.getLoggerName());
        sb.append(" - ");
        sb.append(event.getFormattedMessage());
        sb.append(CoreConstants.LINE_SEPARATOR);
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            String stackTrace = tpc.convert(event);
            sb.append(stackTrace);
        }
        return sb.toString();
    }

}
