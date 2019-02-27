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
package ch.qos.logback.classic.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static java.util.regex.Pattern.quote;

import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.status.ErrorStatus;

/**
 * This converter outputs caller data depending on depth or depth range and marker data.
 * 
 * @author Ceki Gulcu
 */
public class CallerDataConverter extends ClassicConverter {

  public static final String DEFAULT_CALLER_LINE_PREFIX = "Caller+";

  public static final String DEFAULT_RANGE_DELIMITER = "..";

  private int depthStart = 0;
  private int depthEnd = 5;
  List<EventEvaluator<ILoggingEvent>> evaluatorList = null;


  final int MAX_ERROR_COUNT = 4;
  int errorCount = 0;

  @SuppressWarnings("unchecked")
  public void start() {
    String depthStr = getFirstOption();
    if (depthStr == null) {
      return;
    }

    try {
      if (isRange(depthStr)) {
        String[] numbers = splitRange(depthStr);
        if (numbers.length == 2) {
          depthStart = Integer.parseInt(numbers[0]);
          depthEnd = Integer.parseInt(numbers[1]);
          checkRange();
        } else {
          addError("Failed to parse depth option as range [" + depthStr + "]");
        }
      } else {
        depthEnd = Integer.parseInt(depthStr);
      }
    } catch (NumberFormatException nfe) {
      addError("Failed to parse depth option [" + depthStr + "]", nfe);
    }

    final List<String> optionList = getOptionList();

    if (optionList != null && optionList.size() > 1) {
      final int optionListSize = optionList.size();
      for (int i = 1; i < optionListSize; i++) {
        String evaluatorStr = optionList.get(i);
        Context context = getContext();
        if (context != null) {
          Map<String, EventEvaluator<?>> evaluatorMap = (Map<String, EventEvaluator<?>>) context
              .getObject(CoreConstants.EVALUATOR_MAP);
          EventEvaluator<ILoggingEvent> ee = (EventEvaluator<ILoggingEvent>) evaluatorMap
              .get(evaluatorStr);
          if (ee != null) {
            addEvaluator(ee);
          }
        }
      }
    }
  }

  private boolean isRange(String depthStr) {
    return depthStr.contains(getDefaultRangeDelimiter());
  }

  private String[] splitRange(String depthStr) {
    return depthStr.split(quote(getDefaultRangeDelimiter()), 2);
  }

  private void checkRange() {
    if (depthStart < 0 || depthEnd < 0) {
      addError("Invalid depthStart/depthEnd range [" + depthStart + ", " + depthEnd + "] (negative values are not allowed)");
    } else if (depthStart >= depthEnd) {
      addError("Invalid depthEnd range [" + depthStart + ", " + depthEnd + "] (start greater or equal to end)");
    }
  }

  private void addEvaluator(EventEvaluator<ILoggingEvent> ee) {
    if (evaluatorList == null) {
      evaluatorList = new ArrayList<EventEvaluator<ILoggingEvent>>();
    }
    evaluatorList.add(ee);
  }

  public String convert(ILoggingEvent le) {
    StringBuilder buf = new StringBuilder();

    if (evaluatorList != null) {
      boolean printCallerData = false;
      for (int i = 0; i < evaluatorList.size(); i++) {
        EventEvaluator<ILoggingEvent> ee = evaluatorList.get(i);
        try {
          if (ee.evaluate(le)) {
            printCallerData = true;
            break;
          }
        } catch (EvaluationException eex) {
          errorCount++;
          if (errorCount < MAX_ERROR_COUNT) {
            addError("Exception thrown for evaluator named [" + ee.getName()
                + "]", eex);
          } else if (errorCount == MAX_ERROR_COUNT) {
            ErrorStatus errorStatus = new ErrorStatus(
                "Exception thrown for evaluator named [" + ee.getName() + "].",
                this, eex);
            errorStatus.add(new ErrorStatus(
                "This was the last warning about this evaluator's errors."
                    + "We don't want the StatusManager to get flooded.", this));
            addStatus(errorStatus);
          }

        }
      }

      if (!printCallerData) {
        return CoreConstants.EMPTY_STRING;
      }
    }

    StackTraceElement[] cda = le.getCallerData();
    if (cda != null && cda.length > depthStart) {
      int limit = depthEnd < cda.length ? depthEnd : cda.length;

      for (int i = depthStart; i < limit; i++) {
        buf.append(getCallerLinePrefix());
        buf.append(i);
        buf.append("\t at ");
        buf.append(cda[i]);
        buf.append(CoreConstants.LINE_SEPARATOR);
      }
      return buf.toString();
    } else {
      return CallerData.CALLER_DATA_NA;
    }
  }

  protected String getCallerLinePrefix() {
    return DEFAULT_CALLER_LINE_PREFIX;
  }

  protected String getDefaultRangeDelimiter() {
    return DEFAULT_RANGE_DELIMITER;
  }
}
