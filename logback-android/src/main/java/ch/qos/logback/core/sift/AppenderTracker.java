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
package ch.qos.logback.core.sift;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.helpers.NOPAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.AbstractComponentTracker;
import ch.qos.logback.core.spi.ContextAwareImpl;

/**
 * Track appenders by key. When an appender is not used for
 * longer than {@link #DEFAULT_TIMEOUT} it is stopped and removed.
 *
 * @author Tommy Becker
 * @author Ceki Gulcu
 * @author David Roussel
 */
public class AppenderTracker<E> extends AbstractComponentTracker<Appender<E>> {

  int nopaWarningCount = 0;

  final Context context;
  final AppenderFactory<E> appenderFactory;
  final ContextAwareImpl contextAware;

  public AppenderTracker(Context context, AppenderFactory<E> appenderFactory) {
    super();
    this.context = context;
    this.appenderFactory = appenderFactory;
    this.contextAware = new ContextAwareImpl(context, this);
  }


  @Override
  protected void processPriorToRemoval(Appender<E> component) {
    component.stop();
  }

  @Override
  protected Appender<E> buildComponent(String key) {
    Appender<E> appender = null;
    try {
      appender = appenderFactory.buildAppender(context, key);
    } catch (JoranException je) {
      contextAware.addError("Error while building appender with discriminating value [" + key + "]");
    }
    if (appender == null) {
      appender = buildNOPAppender(key);
    }

    return appender;
  }

  private NOPAppender<E> buildNOPAppender(String key) {
    if (nopaWarningCount < CoreConstants.MAX_ERROR_COUNT) {
      nopaWarningCount++;
      contextAware.addError("Building NOPAppender for discriminating value [" + key + "]");
    }
    NOPAppender<E> nopa = new NOPAppender<E>();
    nopa.setContext(context);
    nopa.start();
    return nopa;
  }

  @Override
  protected boolean isComponentStale(Appender<E> appender) {
    return !appender.isStarted();
  }

}
