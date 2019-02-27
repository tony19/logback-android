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
package ch.qos.logback.core.recovery;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class ResilientOutputStreamTest {

  int diff = RandomUtil.getPositiveInt();
  Context context = new ContextBase();

  @BeforeClass
  public static void setUp() {
    File file = new File(CoreTestConstants.OUTPUT_DIR_PREFIX);
    file.mkdirs();
  }

  @Test
  public void verifyRecuperationAfterFailure() throws Exception {
    File file = new File(CoreTestConstants.OUTPUT_DIR_PREFIX+"resilient"+diff+".log");
    ResilientFileOutputStream rfos = new ResilientFileOutputStream(file, true, FileAppender.DEFAULT_BUFFER_SIZE);
    rfos.setContext(context);

    ResilientFileOutputStream spy = spy(rfos);

    spy.write("a".getBytes());
    spy.flush();

    spy.getChannel().close();
    spy.write("b".getBytes());
    spy.flush();
    Thread.sleep(RecoveryCoordinator.BACKOFF_COEFFICIENT_MIN+10);
    spy.write("c".getBytes());
    spy.flush();
    verify(spy).openNewOutputStream();
  }

}
