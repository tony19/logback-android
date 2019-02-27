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

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.util.COWArrayList;

/**
 * Implementation of FilterAttachable.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
final public class FilterAttachableImpl<E> implements FilterAttachable<E> {

  @SuppressWarnings("unchecked")
  COWArrayList<Filter<E>> filterList = new COWArrayList<Filter<E>>(new Filter[0]);

  /**
   * Add a filter to end of the filter list.
   */
  public void addFilter(Filter<E> newFilter) {
    filterList.add(newFilter);
  }

  /**
   * Clear the filter chain
   */
  public void clearAllFilters() {
    filterList.clear();
  }

  /**
   * Loop through the filters in the list. As soon as a filter decides on
   * ACCEPT or DENY, then that value is returned. If all of the filters return
   * NEUTRAL, then NEUTRAL is returned.
   */
  public FilterReply getFilterChainDecision(E event) {
    final Filter<E>[] filterArrray = filterList.asTypedArray();
    final int len = filterArrray.length;

    for (int i = 0; i < len; i++) {
      final FilterReply r = filterArrray[i].decide(event);
      if (r == FilterReply.DENY || r == FilterReply.ACCEPT) {
        return r;
      }
    }

    // no decision
    return FilterReply.NEUTRAL;
  }

  public List<Filter<E>> getCopyOfAttachedFiltersList() {
    return new ArrayList<Filter<E>>(filterList);
  }
}
