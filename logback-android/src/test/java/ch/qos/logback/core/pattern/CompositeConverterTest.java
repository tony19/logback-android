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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class CompositeConverterTest {

  /** Wraps the children's output in angle brackets. */
  static class BracketCompositeConverter extends CompositeConverter<Object> {
    @Override
    protected String transform(Object event, String in) {
      return "<" + in + ">";
    }
  }

  /** A child converter with a recognizable toString(). */
  static class NamedConverter extends Converter<Object> {
    private final String name;

    NamedConverter(String name) {
      this.name = name;
    }

    @Override
    public String convert(Object event) {
      return name;
    }

    @Override
    public String toString() {
      return "Named(" + name + ")";
    }
  }

  @Test
  public void childConverterIsNullUntilSet() {
    BracketCompositeConverter cc = new BracketCompositeConverter();
    assertNull(cc.getChildConverter());
    Converter<Object> child = new NamedConverter("a");
    cc.setChildConverter(child);
    assertSame(child, cc.getChildConverter());
  }

  @Test
  public void convertTransformsTheOutputOfTheWholeChildChain() {
    BracketCompositeConverter cc = new BracketCompositeConverter();
    Converter<Object> first = new NamedConverter("a");
    first.setNext(new NamedConverter("b"));
    cc.setChildConverter(first);
    assertEquals("<ab>", cc.convert(new Object()));
  }

  @Test
  public void convertWithoutChildrenTransformsTheEmptyString() {
    assertEquals("<>", new BracketCompositeConverter().convert(new Object()));
  }

  @Test
  public void toStringWithoutFormattingInfoOrChildren() {
    assertEquals("CompositeConverter<>", new BracketCompositeConverter().toString());
  }

  @Test
  public void toStringShowsFormattingInfo() {
    BracketCompositeConverter cc = new BracketCompositeConverter();
    cc.setFormattingInfo(new FormatInfo(1, 5, false, true));
    assertEquals("CompositeConverter<FormatInfo(1, 5, false, true)>", cc.toString());
  }

  @Test
  public void toStringShowsChildren() {
    BracketCompositeConverter cc = new BracketCompositeConverter();
    cc.setChildConverter(new NamedConverter("a"));
    assertEquals("CompositeConverter<, children: Named(a)>", cc.toString());
  }

  @Test
  public void toStringShowsFormattingInfoAndChildren() {
    BracketCompositeConverter cc = new BracketCompositeConverter();
    cc.setFormattingInfo(new FormatInfo(2, 3, true, false));
    cc.setChildConverter(new NamedConverter("x"));
    assertEquals("CompositeConverter<FormatInfo(2, 3, true, false), children: Named(x)>", cc.toString());
  }
}
