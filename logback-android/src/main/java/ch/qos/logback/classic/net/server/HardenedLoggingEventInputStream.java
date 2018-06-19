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
package ch.qos.logback.classic.net.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.helpers.BasicMarker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ClassPackagingData;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggerRemoteView;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyVO;
import ch.qos.logback.core.net.HardenedObjectInputStream;

public class HardenedLoggingEventInputStream extends HardenedObjectInputStream {

    static public List<String> getWhilelist() {
        List<String> whitelist = new ArrayList<String>();
        whitelist.add(LoggingEventVO.class.getName());
        whitelist.add(LoggerContextVO.class.getName());
        whitelist.add(LoggerRemoteView.class.getName());
        whitelist.add(ThrowableProxyVO.class.getName());
        whitelist.add(BasicMarker.class.getName());
        whitelist.add(Level.class.getName());
        whitelist.add(Logger.class.getName());
        whitelist.add(StackTraceElement.class.getName());
        whitelist.add(StackTraceElement[].class.getName());
        whitelist.add(ThrowableProxy.class.getName());
        whitelist.add(ThrowableProxy[].class.getName());
        whitelist.add(IThrowableProxy.class.getName());
        whitelist.add(IThrowableProxy[].class.getName());
        whitelist.add(StackTraceElementProxy.class.getName());
        whitelist.add(StackTraceElementProxy[].class.getName());
        whitelist.add(ClassPackagingData.class.getName());

        return whitelist;
    }

    public HardenedLoggingEventInputStream(InputStream is) throws IOException {
        super(is, getWhilelist());
    }

    public HardenedLoggingEventInputStream(InputStream is, List<String> additionalAuthorizedClasses) throws IOException {
        this(is);
        super.addToWhitelist(additionalAuthorizedClasses);
    }
}
