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
package ch.qos.logback.classic.spi.special;

import ch.qos.logback.classic.spi.CPDCSpecial;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.PackagingDataCalculator;
import ch.qos.logback.classic.spi.ThrowableProxy;


public class CPDCSpecialImpl implements CPDCSpecial {

  
  Throwable throwable;
  IThrowableProxy throwableProxy;
  
  public void doTest() {
    nesting();
  }
  
  private void nesting() {
    throwable = new Throwable("x");
    throwableProxy = new ThrowableProxy(throwable);
    PackagingDataCalculator pdc = new PackagingDataCalculator();
    pdc.calculate(throwableProxy);
  }
  
  public Throwable getThrowable() {
    return throwable;
  }
  public IThrowableProxy getThrowableProxy() {
    return throwableProxy;
  }
}
