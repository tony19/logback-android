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
package ch.qos.logback.classic.spi;

/**
 * A class whose static initialization always fails, so that once a first attempt
 * failed, {@code Class.forName} throws a {@link NoClassDefFoundError} for it. Used by
 * {@link PackagingDataCalculatorTest}.
 */
public class PackagingDataCalculatorFailingInit {

  static final Object VALUE = fail();

  private static Object fail() {
    throw new IllegalStateException("initialization of PackagingDataCalculatorFailingInit always fails");
  }
}
