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

import java.io.IOException;
import java.io.ObjectOutputStream;

public class InstrumentedObjectOutputStream extends ObjectOutputStream {

    protected InstrumentedObjectOutputStream() throws IOException, SecurityException {
    }

    @Override
    protected void writeObjectOverride(final Object obj) throws IOException {
        // nop
    }

    @Override
    public void flush() throws IOException {
        // nop
    }

    @Override
    public void reset() throws IOException {
        // nop
    }
}
