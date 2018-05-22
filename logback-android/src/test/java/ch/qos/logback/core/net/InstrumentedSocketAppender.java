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
