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
package ch.qos.logback.core.spi;


/**
 *
 * This enum represents the possible replies that a filtering component
 * in logback can return. It is used by implementations of both 
 * {@link ch.qos.logback.core.filter.Filter} and
 * {@link ch.qos.logback.classic.turbo.TurboFilter} abstract classes.
 * 
 * Based on the order that the FilterReply values are declared,
 * FilterReply.ACCEPT.compareTo(FilterReply.DENY) will return 
 * a positive value.
 *
 * @author S&eacute;bastien Pennec
 */
public enum FilterReply {
  DENY,
  NEUTRAL,
  ACCEPT;
}
