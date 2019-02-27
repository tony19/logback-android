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

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

public class CachingFotmatterTest {

  final static String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm";

  SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
  TimeZone perthTZ = TimeZone.getTimeZone("Australia/Perth");
  TimeZone utcTZ = TimeZone.getTimeZone("UTC");

  @Before
  public void setUp() {
    sdf.setTimeZone(utcTZ);
  }

  @Test
  public void timeZoneIsTakenIntoAccount() throws ParseException {

    CachingDateFormatter cdf = new CachingDateFormatter(DATE_PATTERN);
    TimeZone perthTZ = TimeZone.getTimeZone("Australia/Perth");
    cdf.setTimeZone(perthTZ);

    Date march26_2015_0949_UTC = sdf.parse("2015-03-26T09:49");
    System.out.print(march26_2015_0949_UTC);

    String result = cdf.format(march26_2015_0949_UTC.getTime());
    // AWST (Perth) is 8 hours ahead of UTC
    assertEquals("2015-03-26T17:49", result);
  }
}
