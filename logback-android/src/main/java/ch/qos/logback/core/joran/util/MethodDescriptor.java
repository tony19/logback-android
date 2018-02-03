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

import java.lang.reflect.Method;

/**
 * A {@code MethodDescriptor} describes a particular method that a class
 * supports for external access from other components.
 * 
 * @author Anthony K. Trinh
 */
public class MethodDescriptor {
  private String name;
  private Method method;

  public MethodDescriptor(String name, Method method) {
    this.name = name;
    this.method = method;
  }

  public String getName() {
    return name;
  }

  public Method getMethod() {
    return method;
  }
}
