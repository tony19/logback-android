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
package ch.qos.logback.core.rolling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.FileSize;

public class SizeAndTimeBasedRollingPolicyTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  Context context = new ContextBase();
  StatusChecker checker = new StatusChecker(context);
  RollingFileAppender<Object> rfa = new RollingFileAppender<Object>();
  SizeAndTimeBasedRollingPolicy<Object> policy = new SizeAndTimeBasedRollingPolicy<Object>();

  @Before
  public void setUp() {
    rfa.setContext(context);
    policy.setContext(context);
    policy.setParent(rfa);
    policy.setFileNamePattern(new File(tmp.getRoot(), "app-%d{yyyy-MM-dd}.%i.log").getAbsolutePath());
  }

  @Test
  public void startWithoutMaxFileSizeIsRefused() {
    policy.start();

    assertFalse(policy.isStarted());
    assertNull(policy.getTimeBasedFileNamingAndTriggeringPolicy());
    checker.assertContainsMatch(Status.ERROR, "maxFileSize property is mandatory");
  }

  @Test
  public void totalSizeCapBelowMaxFileSizeIsRefused() {
    policy.setMaxFileSize(new FileSize(2048));
    policy.setTotalSizeCap(new FileSize(1024));

    policy.start();

    assertFalse(policy.isStarted());
    checker.assertContainsMatch(Status.ERROR,
        Pattern.quote("totalSizeCap of [1 KB] is smaller than maxFileSize [2 KB] which is non-sensical"));
  }

  @Test
  public void totalSizeCapOfAtLeastMaxFileSizeIsAccepted() {
    policy.setMaxFileSize(new FileSize(1024));
    policy.setTotalSizeCap(new FileSize(1024));

    policy.start();

    assertTrue(policy.isStarted());
    checker.assertContainsMatch(Status.INFO, Pattern.quote("Archive files will be limited to [1 KB] each."));
    SizeAndTimeBasedFNATP<Object> fnatp = (SizeAndTimeBasedFNATP<Object>) policy.getTimeBasedFileNamingAndTriggeringPolicy();
    assertSame(policy.maxFileSize, fnatp.maxFileSize);
    policy.stop();
  }

  @Test
  public void unboundedTotalSizeCapIsAccepted() {
    policy.setMaxFileSize(new FileSize(1024));

    policy.start();

    assertTrue(policy.isStarted());
    policy.stop();
  }

  @Test
  public void toStringIdentifiesTheInstance() {
    assertEquals("c.q.l.core.rolling.SizeAndTimeBasedRollingPolicy@" + policy.hashCode(), policy.toString());
  }
}
