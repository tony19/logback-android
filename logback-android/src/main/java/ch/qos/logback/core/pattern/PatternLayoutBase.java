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
package ch.qos.logback.core.pattern;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.StatusManager;

import java.util.HashMap;
import java.util.Map;


abstract public class PatternLayoutBase<E> extends LayoutBase<E> {

  static final int INTIAL_STRING_BUILDER_SIZE = 256;

  Converter<E> head;
  String pattern;
  protected PostCompileProcessor<E> postCompileProcessor;

  Map<String, String> instanceConverterMap = new HashMap<String, String>();
  protected boolean outputPatternAsHeader = false;

  /**
   * Concrete implementations of this class are responsible for elaborating the
   * mapping between pattern words and converters.
   *
   * @return A map associating pattern words to the names of converter classes
   */
  abstract public Map<String, String> getDefaultConverterMap();

  /**
   * Returns a map where the default converter map is merged with the map
   * contained in the context.
   * @return the effective converter map
   */
  public Map<String, String> getEffectiveConverterMap() {
    Map<String, String> effectiveMap = new HashMap<String, String>();

    // add the least specific map fist
    Map<String, String> defaultMap = getDefaultConverterMap();
    if (defaultMap != null) {
      effectiveMap.putAll(defaultMap);
    }

    // contextMap is more specific than the default map
    Context context = getContext();
    if (context != null) {
      @SuppressWarnings("unchecked")
      Map<String, String> contextMap = (Map<String, String>) context
          .getObject(CoreConstants.PATTERN_RULE_REGISTRY);
      if (contextMap != null) {
        effectiveMap.putAll(contextMap);
      }
    }
    // set the most specific map last
    effectiveMap.putAll(instanceConverterMap);
    return effectiveMap;
  }

  public void start() {
    if(pattern == null || pattern.length() == 0) {
      addError("Empty or null pattern.");
      return;
    }
    try {
      Parser<E> p = new Parser<E>(pattern);
      if (getContext() != null) {
        p.setContext(getContext());
      }
      Node t = p.parse();
      this.head = p.compile(t, getEffectiveConverterMap());
      if (postCompileProcessor != null) {
        postCompileProcessor.process(context, head);
      }
      ConverterUtil.setContextForConverters(getContext(), head);
      ConverterUtil.startConverters(this.head);
      super.start();
    } catch (ScanException sce) {
      StatusManager sm = getContext().getStatusManager();
      sm.add(new ErrorStatus("Failed to parse pattern \"" + getPattern()
          + "\".", this, sce));
    }
  }

  public void setPostCompileProcessor(
      PostCompileProcessor<E> postCompileProcessor) {
    this.postCompileProcessor = postCompileProcessor;
  }

  /**
   *
   * @param head the head node of the converter chain
   * @deprecated  Use {@link ConverterUtil#setContextForConverters} instead. This method will
   *  be removed in future releases.
   */
  protected void setContextForConverters(Converter<E> head) {
    ConverterUtil.setContextForConverters(getContext(), head);
  }

  protected String writeLoopOnConverters(E event) {
    StringBuilder strBuilder = new StringBuilder(INTIAL_STRING_BUILDER_SIZE);
    Converter<E> c = head;
    while (c != null) {
      c.write(strBuilder, event);
      c = c.getNext();
    }
    return strBuilder.toString();
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String toString() {
    return this.getClass().getName() + "(\"" + getPattern() + "\")";
  }

  public Map<String, String> getInstanceConverterMap() {
    return instanceConverterMap;
  }

  protected String getPresentationHeaderPrefix() {
    return CoreConstants.EMPTY_STRING;
  }

  public boolean isOutputPatternAsHeader() {
    return outputPatternAsHeader;
  }

  public void setOutputPatternAsHeader(boolean outputPatternAsHeader) {
    this.outputPatternAsHeader = outputPatternAsHeader;
  }

  @Override
  public String getPresentationHeader() {
    if(outputPatternAsHeader)
      return getPresentationHeaderPrefix()+pattern;
    else
      return super.getPresentationHeader();
  }
}
