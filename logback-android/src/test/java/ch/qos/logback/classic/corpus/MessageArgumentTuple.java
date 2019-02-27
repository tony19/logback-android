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
package ch.qos.logback.classic.corpus;

public class MessageArgumentTuple {
  
  final String message;
  final int numberOfArguments;

  MessageArgumentTuple(String message) {
    this(message, 0);
  }

  public MessageArgumentTuple(String message, int numberOfArguments) {
    this.message = message;
    this.numberOfArguments = numberOfArguments;
  }

}
