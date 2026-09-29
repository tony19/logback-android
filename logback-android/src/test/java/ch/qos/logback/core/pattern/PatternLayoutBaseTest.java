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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.status.Status;

public class PatternLayoutBaseTest {

  /** A layout whose default converter map is supplied by the test. */
  static class MapPatternLayout extends PatternLayoutBase<Object> {
    private final Map<String, String> defaultMap;

    MapPatternLayout(Map<String, String> defaultMap) {
      this.defaultMap = defaultMap;
    }

    @Override
    public Map<String, String> getDefaultConverterMap() {
      return defaultMap;
    }

    @Override
    public String doLayout(Object event) {
      return writeLoopOnConverters(event);
    }
  }

  /** A layout that prefixes the pattern header. */
  static class PrefixedPatternLayout extends MapPatternLayout {
    PrefixedPatternLayout() {
      super(new HashMap<String, String>());
    }

    @Override
    protected String getPresentationHeaderPrefix() {
      return "#pattern: ";
    }
  }

  private static Map<String, String> helloAndOttMap() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("hello", ConverterHello.class.getName());
    map.put("OTT", Converter123.class.getName());
    return map;
  }

  @Test
  public void effectiveConverterMapWithoutDefaultMapOrContextHoldsInstanceEntries() {
    MapPatternLayout layout = new MapPatternLayout(null);
    layout.getInstanceConverterMap().put("x", "some.Converter");

    Map<String, String> expected = new HashMap<String, String>();
    expected.put("x", "some.Converter");
    assertEquals(expected, layout.getEffectiveConverterMap());
  }

  @Test
  public void effectiveConverterMapLetsContextOverrideDefaultAndInstanceOverrideBoth() {
    Map<String, String> defaultMap = new HashMap<String, String>();
    defaultMap.put("a", "default.A");
    defaultMap.put("b", "default.B");
    defaultMap.put("c", "default.C");
    MapPatternLayout layout = new MapPatternLayout(defaultMap);

    Context context = new ContextBase();
    Map<String, String> registry = new HashMap<String, String>();
    registry.put("b", "context.B");
    registry.put("c", "context.C");
    context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
    layout.setContext(context);
    layout.getInstanceConverterMap().put("c", "instance.C");

    Map<String, String> expected = new HashMap<String, String>();
    expected.put("a", "default.A");
    expected.put("b", "context.B");
    expected.put("c", "instance.C");
    assertEquals(expected, layout.getEffectiveConverterMap());
  }

  @Test
  public void effectiveConverterMapIgnoresContextWithoutRegistry() {
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setContext(new ContextBase());
    assertEquals(helloAndOttMap(), layout.getEffectiveConverterMap());
  }

  @Test
  public void startWithoutContextCompilesLiteralPatterns() {
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setPattern("just text");
    layout.start();

    assertTrue(layout.isStarted());
    assertNull(layout.getContext());
    assertEquals("just text", layout.doLayout(new Object()));
  }

  @Test
  public void startWithContextCompilesAndStartsTheConverters() {
    Context context = new ContextBase();
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setContext(context);
    layout.getInstanceConverterMap().put("EX", ExceptionalConverter.class.getName());
    layout.setPattern("%hello-%OTT%EX");
    layout.start();

    assertTrue(layout.isStarted());
    assertEquals("Hello-123", layout.doLayout(new Object()));
    assertTrue(context.getStatusManager().getCopyOfStatusList().isEmpty());
  }

  @Test
  public void startReportsAnUnparsablePatternAndStaysStopped() {
    Context context = new ContextBase();
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setContext(context);
    layout.setPattern("%hello%(abc");
    layout.start();

    assertFalse(layout.isStarted());
    assertEquals("", layout.doLayout(new Object()));
    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    Status last = statuses.get(statuses.size() - 1);
    assertEquals(Status.ERROR, last.getLevel());
    assertEquals("Failed to parse pattern \"%hello%(abc\".", last.getMessage());
    assertSame(layout, last.getOrigin());
    assertTrue(last.getThrowable() instanceof ScanException);
  }

  @Test
  public void startRunsThePostCompileProcessorOnTheCompiledChain() {
    final Context context = new ContextBase();
    final List<Context> seenContexts = new ArrayList<Context>();
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setContext(context);
    layout.setPattern("%hello");
    layout.setPostCompileProcessor(new PostCompileProcessor<Object>() {
      @Override
      public void process(Context ctx, Converter<Object> head) {
        seenContexts.add(ctx);
        ConverterUtil.findTail(head).setNext(new LiteralConverter<Object>("!"));
      }
    });
    layout.start();

    assertTrue(layout.isStarted());
    assertEquals(Collections.singletonList(context), seenContexts);
    assertEquals("Hello!", layout.doLayout(new Object()));
  }

  @Test
  public void deprecatedSetContextForConvertersSetsTheLayoutContextOnTheChain() {
    Context context = new ContextBase();
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setContext(context);
    ConverterHello first = new ConverterHello();
    Converter123 second = new Converter123();
    first.setNext(second);

    layout.setContextForConverters(first);

    assertSame(context, first.getContext());
    assertSame(context, second.getContext());
  }

  @Test
  public void toStringShowsClassNameAndPattern() {
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setPattern("%hello %OTT");
    assertEquals(MapPatternLayout.class.getName() + "(\"%hello %OTT\")", layout.toString());
  }

  @Test
  public void outputPatternAsHeaderIsOffByDefault() {
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setPattern("%hello");
    assertFalse(layout.isOutputPatternAsHeader());
    assertNull(layout.getPresentationHeader());
  }

  @Test
  public void presentationHeaderFallsBackToTheConfiguredHeaderWhenPatternIsNotOutput() {
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setPattern("%hello");
    layout.setPresentationHeader("my header");
    assertEquals("my header", layout.getPresentationHeader());
  }

  @Test
  public void presentationHeaderIsThePatternWhenOutputPatternAsHeaderIsSet() {
    MapPatternLayout layout = new MapPatternLayout(helloAndOttMap());
    layout.setPattern("%hello %OTT");
    layout.setPresentationHeader("ignored");
    layout.setOutputPatternAsHeader(true);

    assertTrue(layout.isOutputPatternAsHeader());
    assertEquals("%hello %OTT", layout.getPresentationHeader());
  }

  @Test
  public void presentationHeaderPrefixIsPrependedToThePattern() {
    PrefixedPatternLayout layout = new PrefixedPatternLayout();
    layout.setPattern("%msg");
    layout.setOutputPatternAsHeader(true);
    assertEquals("#pattern: %msg", layout.getPresentationHeader());
  }
}
