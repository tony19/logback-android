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
package ch.qos.logback.core.pattern.color;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ch.qos.logback.core.pattern.LiteralConverter;

/**
 * Checks that every color composite converter wraps its children's output in
 * the ANSI escape sequence of its color and resets to the default color after.
 */
@RunWith(Parameterized.class)
public class ForegroundCompositeConverterBaseTest {

  private static final String RESET = "\u001b[0;39m";

  @Parameters(name = "{0}")
  public static Collection<Object[]> converters() {
    return Arrays.asList(new Object[][] {
        { "black", BlackCompositeConverter.class, "30" },
        { "red", RedCompositeConverter.class, "31" },
        { "green", GreenCompositeConverter.class, "32" },
        { "yellow", YellowCompositeConverter.class, "33" },
        { "blue", BlueCompositeConverter.class, "34" },
        { "magenta", MagentaCompositeConverter.class, "35" },
        { "cyan", CyanCompositeConverter.class, "36" },
        { "white", WhiteCompositeConverter.class, "37" },
        { "gray", GrayCompositeConverter.class, "1;30" },
        { "boldRed", BoldRedCompositeConverter.class, "1;31" },
        { "boldGreen", BoldGreenCompositeConverter.class, "1;32" },
        { "boldYellow", BoldYellowCompositeConverter.class, "1;33" },
        { "boldBlue", BoldBlueCompositeConverter.class, "1;34" },
        { "boldMagenta", BoldMagentaCompositeConverter.class, "1;35" },
        { "boldCyan", BoldCyanCompositeConverter.class, "1;36" },
        { "boldWhite", BoldWhiteCompositeConverter.class, "1;37" },
    });
  }

  private final ForegroundCompositeConverterBase<Object> converter;
  private final String colorCode;

  @SuppressWarnings("unchecked")
  public ForegroundCompositeConverterBaseTest(String name, Class<?> converterClass, String colorCode)
      throws Exception {
    // a fresh converter for every test
    this.converter = (ForegroundCompositeConverterBase<Object>) converterClass.getConstructor().newInstance();
    this.colorCode = colorCode;
  }

  @Test
  public void foregroundColorCode() {
    assertEquals(colorCode, converter.getForegroundColorCode(new Object()));
  }

  @Test
  public void wrapsChildOutputInColorAndResetSequences() {
    converter.setChildConverter(new LiteralConverter<Object>("text"));
    converter.start();
    assertEquals("\u001b[" + colorCode + "m" + "text" + RESET, converter.convert(new Object()));
  }
}
