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
package ch.qos.logback.core.net;

import java.net.InetAddress;

import ch.qos.logback.core.spi.PreSerializationTransformer;

public class InstrumentedSocketAppender extends AbstractSocketAppender<String> {

    private PreSerializationTransformer<String> preSerializationTransformer;
    private SocketConnector socketConnector;

    public InstrumentedSocketAppender(PreSerializationTransformer<String> preSerializationTransformer,
                                      QueueFactory queueFactory,
                                      ObjectWriterFactory objectWriterFactory,
                                      SocketConnector socketConnector) {
        super(queueFactory, objectWriterFactory);
        this.preSerializationTransformer = preSerializationTransformer;
        this.socketConnector = socketConnector;
    }

    @Override
    protected void postProcessEvent(String event) {
    }

    @Override
    protected PreSerializationTransformer<String> getPST() {
        return preSerializationTransformer;
    }

    @Override
    protected SocketConnector newConnector(InetAddress address, int port, long initialDelay, long retryDelay) {
        return socketConnector;
    }
}
