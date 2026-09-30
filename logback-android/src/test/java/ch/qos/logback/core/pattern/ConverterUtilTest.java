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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;

public class ConverterUtilTest {

  @Test
  public void isInstantiable() {
    // the implicit public constructor is part of the published API
    assertNotNull(new ConverterUtil());
  }

  @Test
  public void findTailOfNullChainIsNull() {
    assertNull(ConverterUtil.findTail(null));
  }

  @Test
  public void findTailOfSingleConverterIsThatConverter() {
    Converter<Object> head = new LiteralConverter<Object>("a");
    assertSame(head, ConverterUtil.findTail(head));
  }

  @Test
  public void findTailReturnsTheLastConverterOfTheChain() {
    Converter<Object> head = new LiteralConverter<Object>("a");
    Converter<Object> middle = new LiteralConverter<Object>("b");
    Converter<Object> tail = new LiteralConverter<Object>("c");
    head.setNext(middle);
    middle.setNext(tail);
    assertSame(tail, ConverterUtil.findTail(head));
  }

  @Test
  public void startConvertersStartsDynamicAndCompositeConvertersAndTheirChildren() {
    ConverterHello dynamic = new ConverterHello();
    ReplacingCompositeConverter<Object> composite = new ReplacingCompositeConverter<Object>();
    composite.setOptionList(Arrays.asList("a", "b"));
    Converter123 child = new Converter123();
    composite.setChildConverter(child);
    Converter<Object> literal = new LiteralConverter<Object>("-");
    dynamic.setNext(literal);
    literal.setNext(composite);

    ConverterUtil.startConverters(dynamic);

    assertTrue(dynamic.isStarted());
    assertTrue(composite.isStarted());
    assertTrue(child.isStarted());
  }

  @Test
  public void startConvertersLeavesConvertersOutsideTheChainStopped() {
    ConverterHello head = new ConverterHello();
    ConverterHello notLinked = new ConverterHello();

    ConverterUtil.startConverters(head);

    assertTrue(head.isStarted());
    assertFalse(notLinked.isStarted());
  }

  @Test
  public void setContextForConvertersSetsContextOnContextAwareConverters() {
    Context context = new ContextBase();
    ConverterHello first = new ConverterHello();
    Converter<Object> literal = new LiteralConverter<Object>("-");
    Converter123 last = new Converter123();
    first.setNext(literal);
    literal.setNext(last);

    ConverterUtil.setContextForConverters(context, first);

    assertSame(context, first.getContext());
    assertSame(context, last.getContext());
  }
}
