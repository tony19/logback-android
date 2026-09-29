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
package ch.qos.logback.core.rolling.helper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;

public class IntParserTest {

  private final Context context = new ContextBase();

  @Test
  public void parsesIntegerOfMatchingFilename() {
    IntParser parser = new IntParser(new FileNamePattern("/logs/%d{yyyy-MM-dd}/app-%i.log", context));

    assertEquals(Integer.valueOf(12), parser.parseFilename("/logs/2019-11-04/app-12.log"));
  }

  @Test
  public void filenameNotMatchingThePatternParsesAsMinusOne() {
    IntParser parser = new IntParser(new FileNamePattern("/logs/%d{yyyy-MM-dd}/app-%i.log", context));

    assertEquals(Integer.valueOf(-1), parser.parseFilename("/logs/other.txt"));
  }

  @Test
  public void patternWithoutIntegerTokenParsesAsMinusOne() {
    IntParser parser = new IntParser(new FileNamePattern("/logs/app-%d{yyyy-MM-dd}.log", context));

    // the filename matches, but there is no integer token to capture
    assertEquals(Integer.valueOf(-1), parser.parseFilename("/logs/app-2019-11-04.log"));
  }
}
