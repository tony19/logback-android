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
