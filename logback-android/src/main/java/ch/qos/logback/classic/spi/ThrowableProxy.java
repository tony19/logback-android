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

import ch.qos.logback.core.CoreConstants;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class ThrowableProxy implements IThrowableProxy {

  private Throwable throwable;
  private String className;
  private String message;
  // package-private because of ThrowableProxyUtil
  StackTraceElementProxy[] stackTraceElementProxyArray;
  // package-private because of ThrowableProxyUtil
  int commonFrames;
  private ThrowableProxy cause;
  private ThrowableProxy[] suppressed = NO_SUPPRESSED;

  private transient PackagingDataCalculator packagingDataCalculator;
  private boolean calculatedPackageData = false;

  private static final ThrowableProxy[] NO_SUPPRESSED = new ThrowableProxy[0];
  private static final StackTraceElementProxy[] NO_STACK_TRACE = new StackTraceElementProxy[0];

  public ThrowableProxy(Throwable throwable) {
    this(throwable, Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>(1)));
  }

  private ThrowableProxy(Throwable throwable, Set<Throwable> visited) {
    this.throwable = throwable;
    this.className = throwable.getClass().getName();
    this.message = throwable.getMessage();
    this.stackTraceElementProxyArray = ThrowableProxyUtil.steArrayToStepArray(throwable
        .getStackTrace());

    if (visited.contains(throwable)) {
      this.className = "CIRCULAR REFERENCE:" + throwable.getClass().getName();
      this.stackTraceElementProxyArray = NO_STACK_TRACE;
    } else {
      visited.add(throwable);

      Throwable nested = throwable.getCause();
      if (nested != null) {
        this.cause = new ThrowableProxy(nested, visited);
        this.cause.commonFrames = ThrowableProxyUtil
            .findNumberOfCommonFrames(nested.getStackTrace(),
                stackTraceElementProxyArray);
      }

      Throwable[] throwableSuppressed = throwable.getSuppressed();
      // while JDK's implementation of getSuppressed() will always return a non-null array,
      // this might not be the case in mocked throwables or in other implementations
      if (throwableSuppressed != null && throwableSuppressed.length > 0) {
        suppressed = new ThrowableProxy[throwableSuppressed.length];
        for (int i = 0; i < throwableSuppressed.length; i++) {
          this.suppressed[i] = new ThrowableProxy(throwableSuppressed[i], visited);
          this.suppressed[i].commonFrames = ThrowableProxyUtil
                  .findNumberOfCommonFrames(throwableSuppressed[i].getStackTrace(),
                          stackTraceElementProxyArray);
        }
      }
    }

  }


  public Throwable getThrowable() {
    return throwable;
  }

  @Override
  public String getMessage() {
    return message;
  }

  /*
   * (non-Javadoc)
   * 
   * @see ch.qos.logback.classic.spi.IThrowableProxy#getClassName()
   */
  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public StackTraceElementProxy[] getStackTraceElementProxyArray() {
    return stackTraceElementProxyArray;
  }

  @Override
  public int getCommonFrames() {
    return commonFrames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see ch.qos.logback.classic.spi.IThrowableProxy#getCause()
   */
  @Override
  public IThrowableProxy getCause() {
    return cause;
  }

  @Override
  public IThrowableProxy[] getSuppressed() {
    return suppressed;
  }

  public PackagingDataCalculator getPackagingDataCalculator() {
    // if original instance (non-deserialized), and packagingDataCalculator
    // is not already initialized, then create an instance.
    // here we assume that (throwable == null) for deserialized instances
    if (throwable != null && packagingDataCalculator == null) {
      packagingDataCalculator = new PackagingDataCalculator();
    }
    return packagingDataCalculator;
  }

  public void calculatePackagingData() {
    if (calculatedPackageData) {
      return;
    }
    PackagingDataCalculator pdc = this.getPackagingDataCalculator();
    if (pdc != null) {
      calculatedPackageData = true;
      pdc.calculate(this);
    }
  }



  public void fullDump() {
    StringBuilder builder = new StringBuilder();
    for (StackTraceElementProxy step : stackTraceElementProxyArray) {
      String string = step.toString();
      builder.append(CoreConstants.TAB).append(string);
      ThrowableProxyUtil.subjoinPackagingData(builder, step);
      builder.append(CoreConstants.LINE_SEPARATOR);
    }
    System.out.println(builder.toString());
  }


}
