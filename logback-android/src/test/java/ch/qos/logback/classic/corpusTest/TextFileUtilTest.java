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
package ch.qos.logback.classic.corpusTest;

import static junit.framework.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Test;

import ch.qos.logback.classic.corpus.TextFileUtil;

public class TextFileUtilTest {

  @Test
  public void smoke() throws IOException {
    String s = "When on board H.M.S. 'Beagle,' as naturalist, I was much struck with\r\n"
        + "certain facts in the distribution of the inhabitants of South America,\r\n"
        + "and in the geological relations of the present to the past inhabitants\r\n"
        + "of that continent.";
    
    StringReader sr = new StringReader(s);
    BufferedReader br = new BufferedReader(sr);
    List<String> wordList = TextFileUtil.toWords(br);
    assertEquals(38, wordList.size());
    assertEquals("When", wordList.get(0));
    assertEquals("'Beagle,'", wordList.get(4));
    assertEquals("of", wordList.get(17));
    
  }
}
