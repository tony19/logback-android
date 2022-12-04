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
package ch.qos.logback.classic.turbo;

import org.slf4j.Marker;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;

/**
 *
 * See http://logback.qos.ch/manual/filters.html#DuplicateMessageFilter
 * for details.
 *
 * @author Ceki Gulcu
 *
 */
public class DuplicateMessageFilter extends TurboFilter {

  /**
   * The default cache size.
   */
  public static final int DEFAULT_CACHE_SIZE = 100;
  /**
   * The default number of allows repetitions.
   */
  public static final int DEFAULT_ALLOWED_REPETITIONS = 5;

  public int allowedRepetitions = DEFAULT_ALLOWED_REPETITIONS;
  public int cacheSize = DEFAULT_CACHE_SIZE;

  private LRUMessageCache msgCache;

  @Override
  public void start() {
    msgCache = new LRUMessageCache(cacheSize);
    super.start();
  }

  @Override
  public void stop() {
    msgCache.clear();
    msgCache = null;
    super.stop();
  }

  @Override
  public FilterReply decide(List<Marker> markers, Logger logger, Level level,
                            String format, Object[] params, Throwable t) {
    int count = msgCache.getMessageCountAndThenIncrement(format);
    if (count <= allowedRepetitions) {
      return FilterReply.NEUTRAL;
    } else {
      return FilterReply.DENY;
    }
  }

  public int getAllowedRepetitions() {
    return allowedRepetitions;
  }

  /**
   * The allowed number of repetitions before
   *
   * @param allowedRepetitions
   */
  public void setAllowedRepetitions(int allowedRepetitions) {
    this.allowedRepetitions = allowedRepetitions;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

}
