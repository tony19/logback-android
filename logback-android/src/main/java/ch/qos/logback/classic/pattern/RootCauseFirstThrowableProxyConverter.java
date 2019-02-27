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

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;

/**
 * @author Tomasz Nurkiewicz
 * @since 0.9.30
 */
public class RootCauseFirstThrowableProxyConverter extends ExtendedThrowableProxyConverter {

  @Override
  protected String throwableProxyToString(IThrowableProxy tp) {
    StringBuilder buf = new StringBuilder(BUILDER_CAPACITY);
    recursiveAppendRootCauseFirst(buf, null, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, tp);
    return buf.toString();
  }

  protected void recursiveAppendRootCauseFirst(StringBuilder sb, String prefix, int indent, IThrowableProxy tp) {
    if (tp.getCause() != null) {
      recursiveAppendRootCauseFirst(sb, prefix, indent, tp.getCause());
      prefix = null; // to avoid adding it more than once
    }
    ThrowableProxyUtil.indent(sb, indent - 1);
    if (prefix != null) {
      sb.append(prefix);
    }
    ThrowableProxyUtil.subjoinFirstLineRootCauseFirst(sb, tp);
    sb.append(CoreConstants.LINE_SEPARATOR);
    subjoinSTEPArray(sb, indent, tp);
    IThrowableProxy[] suppressed = tp.getSuppressed();
    if(suppressed != null) {
      for(IThrowableProxy current : suppressed) {
        recursiveAppendRootCauseFirst(sb, CoreConstants.SUPPRESSED, indent + ThrowableProxyUtil.SUPPRESSED_EXCEPTION_INDENT, current);
      }
    }
  }
}
