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
 * A {@code PropertyDescriptor} describes a particular property that a class
 * exports via a pair of accessor methods.
 * 
 * @author Anthony K. Trinh
 */
public class PropertyDescriptor {
  private Method writeMethod;
  private Method readMethod;
  private String name;
  private Class<?> type;

  public PropertyDescriptor(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Method getWriteMethod() {
    return writeMethod;
  }

  public void setWriteMethod(Method writeMethod) {
    this.writeMethod = writeMethod;
  }

  public Method getReadMethod() {
    return readMethod;
  }

  public void setReadMethod(Method readMethod) {
    this.readMethod = readMethod;
  }

  public Class<?> getPropertyType() {
    return type;
  }

  public void setPropertyType(Class<?> type) {
    this.type = type;
  }
}
