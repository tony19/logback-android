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
package ch.qos.logback.classic.html;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.helpers.Transform;
import ch.qos.logback.core.html.IThrowableRenderer;

public class DefaultThrowableRenderer implements
    IThrowableRenderer<ILoggingEvent> {

  static final String TRACE_PREFIX = "<br />&nbsp;&nbsp;&nbsp;&nbsp;";

  public void render(StringBuilder sbuf, ILoggingEvent event) {
    IThrowableProxy tp = event.getThrowableProxy();
    sbuf.append("<tr><td class=\"Exception\" colspan=\"6\">");
    while (tp != null) {
      render(sbuf, tp);
      tp = tp.getCause();
    }
    sbuf.append("</td></tr>");
  }

  void render(StringBuilder sbuf, IThrowableProxy tp) {
    printFirstLine(sbuf, tp);
    
    int commonFrames = tp.getCommonFrames();
    StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
    
    for (int i = 0; i < stepArray.length - commonFrames; i++) {
      StackTraceElementProxy step = stepArray[i];
      sbuf.append(TRACE_PREFIX);
      sbuf.append(Transform.escapeTags(step.toString()));
      sbuf.append(CoreConstants.LINE_SEPARATOR);
    }
    
    if (commonFrames > 0) {
      sbuf.append(TRACE_PREFIX);
      sbuf.append("\t... ").append(commonFrames).append(" common frames omitted")
          .append(CoreConstants.LINE_SEPARATOR);
    }
  }

  public void printFirstLine(StringBuilder sb, IThrowableProxy tp) {
    int commonFrames = tp.getCommonFrames();
    if (commonFrames > 0) {
      sb.append("<br />").append(CoreConstants.CAUSED_BY);
    }
    sb.append(tp.getClassName()).append(": ").append(
        Transform.escapeTags(tp.getMessage()));
    sb.append(CoreConstants.LINE_SEPARATOR);
  }

}
