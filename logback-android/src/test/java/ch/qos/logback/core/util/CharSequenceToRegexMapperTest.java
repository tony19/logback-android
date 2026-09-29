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
package ch.qos.logback.core.util;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.text.DateFormatSymbols;
import java.util.Locale;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;


public class CharSequenceToRegexMapperTest {
  static Locale KO_LOCALE = new Locale("ko", "KR");
  Locale oldLocale = Locale.getDefault();

  @After
  public void tearDown() {
    Locale.setDefault(oldLocale);
  }

  @Test
  public void findMinMaxLengthsInSymbolsWithTrivialInputs() {
    String[] symbols = new String[]{"a", "bb"};
    int[] results = CharSequenceToRegexMapper.findMinMaxLengthsInSymbols(symbols);
    assertEquals(1, results[0]);
    assertEquals(2, results[1]);
  }

  @Test
  public void emptyStringValuesShouldBeIgnoredByFindMinMaxLengthsInSymbols() {
    String[] symbols = new String[]{"aaa", ""};
    int[] results = CharSequenceToRegexMapper.findMinMaxLengthsInSymbols(symbols);
    assertEquals(3, results[0]);
    assertEquals(3, results[1]);
  }


  @Test
  @Ignore
  public void noneOfTheSymbolsAreOfZeroLengthForKorean() {
    Locale.setDefault(KO_LOCALE);
    noneOfTheSymbolsAreOfZeroLength();
  }

  @Test
  @Ignore
  public void noneOfTheSymbolsAreOfZeroLengthForSwiss() {
    Locale.setDefault(new Locale("fr", "CH"));
    noneOfTheSymbolsAreOfZeroLength();
  }

  private void noneOfTheSymbolsAreOfZeroLength() {
    DateFormatSymbols dateFormatSymbols = DateFormatSymbols.getInstance();
    //checkEmptyString(dateFormatSymbols.getShortMonths(), "ShortMonths");
    //checkEmptyString(dateFormatSymbols.getMonths(), "Months");
    checkEmptyString(dateFormatSymbols.getShortWeekdays(), "ShortWeekdays");
    checkEmptyString(dateFormatSymbols.getWeekdays(), "Weekdays");
    checkEmptyString(dateFormatSymbols.getAmPmStrings(), "AmPmStrings");

  }

  private void checkEmptyString(String[] symbolArray, String category) {
    for (String s : symbolArray) {
      System.out.println(category + " [" + s + "]");
      assertTrue(category + " contains empty strings", s.length() > 0);
    }
  }

  private static CharSequenceState css(char c, int occurrences) {
    CharSequenceState state = new CharSequenceState(c);
    for (int i = 1; i < occurrences; i++) {
      state.incrementOccurrences();
    }
    return state;
  }

  @Test
  public void backslashIsRejected() {
    final CharSequenceToRegexMapper mapper = new CharSequenceToRegexMapper();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> mapper.toRegex(css('\\', 1)));
    assertEquals("Forward slashes are not allowed", e.getMessage());
  }

  @Test
  public void singleQuoteMapsToEmptyString() {
    assertEquals("", new CharSequenceToRegexMapper().toRegex(css('\'', 1)));
  }

  @Test
  public void repeatedSingleQuotesAreRejected() {
    final CharSequenceToRegexMapper mapper = new CharSequenceToRegexMapper();
    IllegalStateException e = assertThrows(IllegalStateException.class, () -> mapper.toRegex(css('\'', 2)));
    assertEquals("Too many single quotes", e.getMessage());
  }

  @Test
  public void otherCharactersMapToThemselvesRepeatedByOccurrences() {
    CharSequenceToRegexMapper mapper = new CharSequenceToRegexMapper();
    assertEquals("-", mapper.toRegex(css('-', 1)));
    assertEquals("-{3}", mapper.toRegex(css('-', 3)));
  }

}
