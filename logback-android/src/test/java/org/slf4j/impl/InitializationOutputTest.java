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
package org.slf4j.impl;

import ch.qos.logback.classic.ClassicTestConstants;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.status.NopStatusListener;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.TeeOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Ceki G&uuml;c&uuml;
 */
@RunWith(RobolectricTestRunner.class)
public class InitializationOutputTest {

  int diff = RandomUtil.getPositiveInt();

  TeeOutputStream tee;
  PrintStream original;

  @Before
  public void setUp()  {
    original = System.out;
    // tee will output bytes on System out but it will also
    // collect them so that the output can be compared against
    // some expected output data

    // keep the console quiet
    tee = new TeeOutputStream(null);

    // redirect System.out to tee
    System.setOut(new PrintStream(tee));
  }

  @After
  public void tearDown()  {
    System.setOut(original);
    System.clearProperty(ContextInitializer.CONFIG_FILE_PROPERTY);
    System.clearProperty(CoreConstants.STATUS_LISTENER_CLASS);
  }


  @Test
  public void noOutputIfContextHasAStatusListener() {
    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, ClassicTestConstants.INPUT_PREFIX + "issue/logback292.xml");
    System.setProperty(CoreConstants.STATUS_LISTENER_CLASS, NopStatusListener.class.getName());

    StaticLoggerBinderFriend.reset();
    assertEquals(0, tee.baos.size());
  }

}
