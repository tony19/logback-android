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
package ch.qos.logback.core.util;

/**
 * AggregationType classifies how one object is contained within 
 * another object.
 * 
 * 
 * 
 * See also http://en.wikipedia.org/wiki/Class_diagram
 * and http://en.wikipedia.org/wiki/Object_composition
 * 
 * @author Ceki Gulcu
 */
public enum AggregationType {
  NOT_FOUND, 
  AS_BASIC_PROPERTY, // Long, Integer, Double,..., java primitive, String,
                      // Duration or FileSize
  AS_COMPLEX_PROPERTY, // a complex property, a.k.a. attribute, is any attribute 
                       // not covered by basic attributes, i.e. 
                       // object types defined by the user
  AS_BASIC_PROPERTY_COLLECTION, // a collection of basic attributes
  AS_COMPLEX_PROPERTY_COLLECTION; // a collection of complex attributes
}
