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
package ch.qos.logback.core.joran.util;

/**
 * Thrown when an exception happens during Introspection.
 * <p>
 * Typical causes include not being able to map a string class name to a 
 * {@code Class} object, not being able to resolve a string method name, 
 * or specifying a method name that has the wrong type signature for its 
 * intended use. 
 * 
 * @author Anthony K. Trinh
 */
public class IntrospectionException extends RuntimeException {
    private static final long serialVersionUID = -6760181416658938878L;

    public IntrospectionException(String message) {
        super(message);
    }
    
    public IntrospectionException(Exception ex) {
        super(ex);
    }
}
