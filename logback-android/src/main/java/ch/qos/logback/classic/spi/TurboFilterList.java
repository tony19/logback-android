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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Implementation of TurboFilterAttachable.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
final public class TurboFilterList extends CopyOnWriteArrayList<TurboFilter> {

  private static final long serialVersionUID = 1L;

  /**
   * Loop through the filters in the chain. As soon as a filter decides on
   * ACCEPT or DENY, then that value is returned. If all of the filters return
   * NEUTRAL, then NEUTRAL is returned.
   */
  public FilterReply getTurboFilterChainDecision(final List<Marker> markers,
      final Logger logger, final Level level, final String format,
      final Object[] params, final Throwable t) {
    
    
    final int size = size();
//    if (size == 0) {
//      return FilterReply.NEUTRAL;
//    }
    if (size == 1) {
      try {
        TurboFilter tf = get(0);
        return tf.decide(markers, logger, level, format, params, t);
      } catch (IndexOutOfBoundsException iobe) {
        return FilterReply.NEUTRAL;
      }
    }
    
    Object[] tfa = toArray();
    final int len = tfa.length;
    for (int i = 0; i < len; i++) {
    //for (TurboFilter tf : this) {
      final TurboFilter tf = (TurboFilter) tfa[i];
      final FilterReply r = tf.decide(markers, logger, level, format, params, t);
      if (r == FilterReply.DENY || r == FilterReply.ACCEPT) {
        return r;
      }
    }
    return FilterReply.NEUTRAL;
  }

  // public boolean remove(TurboFilter turboFilter) {
  // return tfList.remove(turboFilter);
  // }
  //
  // public TurboFilter remove(int index) {
  // return tfList.remove(index);
  // }
  //
  // final public int size() {
  // return tfList.size();
  // }

}
