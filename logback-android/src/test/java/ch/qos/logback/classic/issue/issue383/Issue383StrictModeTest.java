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
package ch.qos.logback.classic.issue.issue383;

import android.content.Context;
import android.content.ContextWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.android.AndroidContextUtil;
import ch.qos.logback.core.joran.spi.JoranException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

/**
 * Reproduces the scenario from issue #383, where initializing logback through
 * the slf4j API on Android with StrictMode enabled crashed with a
 * {@code DiskReadViolation}. In logback-android 2.0.0, configuration eagerly
 * called {@link AndroidContextUtil#setupProperties(ch.qos.logback.core.Context)},
 * which invokes {@link Context#getFilesDir()} on the calling (main) thread even
 * when no config value referenced {@code ${DATA_DIR}}.
 *
 * <p>Since 3.0.0, those Android properties are resolved lazily: the disk-touching
 * {@code getFilesDir()} is only reached when a config value actually references
 * one of the Android special vars (e.g. {@code ${DATA_DIR}}). This test verifies
 * that behavior directly, standing in for a manual "logback-test-app" repro so it
 * does not have to be reproduced by hand on a device.
 *
 * @see <a href="https://github.com/tony19/logback-android/issues/383">Issue #383</a>
 */
@RunWith(RobolectricTestRunner.class)
public class Issue383StrictModeTest {

  private FilesDirCountingContext trackingContext;

  /**
   * Wraps the real application context and counts every {@link #getFilesDir()}
   * call. On a real device with StrictMode's {@code detectDiskReads()}, each of
   * these calls is what triggers the {@code DiskReadViolation} seen in #383.
   *
   * <p>{@link #getApplicationContext()} returns {@code this} so that the counting
   * wrapper survives {@link AndroidContextUtil}'s internal
   * {@code context.getApplicationContext()} unwrapping.
   */
  private static class FilesDirCountingContext extends ContextWrapper {
    final AtomicInteger getFilesDirCalls = new AtomicInteger(0);

    FilesDirCountingContext(Context base) {
      super(base);
    }

    @Override
    public Context getApplicationContext() {
      return this;
    }

    @Override
    public File getFilesDir() {
      getFilesDirCalls.incrementAndGet();
      return super.getFilesDir();
    }
  }

  @Before
  public void setUp() {
    trackingContext = new FilesDirCountingContext(RuntimeEnvironment.getApplication());
    AndroidContextUtil.setApplicationContext(trackingContext);
  }

  @After
  public void tearDown() {
    AndroidContextUtil.setApplicationContext(null);
  }

  private static void configure(LoggerContext loggerContext, String xml) throws JoranException {
    JoranConfigurator jc = new JoranConfigurator();
    jc.setContext(loggerContext);
    jc.doConfigure(new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
  }

  /**
   * The #383 case: a plain config (no {@code ${DATA_DIR}} reference), just like
   * the default/no-config slf4j initialization the reporter hit. Configuration
   * must NOT touch {@link Context#getFilesDir()}, so StrictMode would not flag a
   * disk read.
   */
  @Test
  public void plainConfigDoesNotTouchGetFilesDir() throws JoranException {
    String xml =
        "<configuration>" +
        "  <appender name='CONSOLE' class='ch.qos.logback.core.ConsoleAppender'>" +
        "    <encoder><pattern>%msg%n</pattern></encoder>" +
        "  </appender>" +
        "  <root level='DEBUG'><appender-ref ref='CONSOLE'/></root>" +
        "</configuration>";

    LoggerContext loggerContext = new LoggerContext();
    configure(loggerContext, xml);

    assertThat("plain configuration must not call Context.getFilesDir() (issue #383)",
        trackingContext.getFilesDirCalls.get(), is(0));
    // The Android property is never resolved, so it stays unset.
    assertThat(loggerContext.getProperty(CoreConstants.DATA_DIR_KEY), is((String) null));
  }

  /**
   * When a config value genuinely references {@code ${DATA_DIR}}, the disk access
   * is inherent (the property has to be resolved). This confirms the lazy path
   * still works: {@code getFilesDir()} is reached, and {@code ${DATA_DIR}} resolves
   * to the real files dir.
   */
  @Test
  public void configReferencingDataDirResolvesLazily() throws JoranException {
    String xml =
        "<configuration>" +
        "  <property name='LOG_DIR' value='${DATA_DIR}/logs' />" +
        "  <appender name='CONSOLE' class='ch.qos.logback.core.ConsoleAppender'>" +
        "    <encoder><pattern>${LOG_DIR} %msg%n</pattern></encoder>" +
        "  </appender>" +
        "  <root level='DEBUG'><appender-ref ref='CONSOLE'/></root>" +
        "</configuration>";

    LoggerContext loggerContext = new LoggerContext();
    configure(loggerContext, xml);

    assertThat("a config referencing ${DATA_DIR} must resolve it (and thus touch getFilesDir())",
        trackingContext.getFilesDirCalls.get(), is(greaterThanOrEqualTo(1)));
    assertThat(loggerContext.getProperty(CoreConstants.DATA_DIR_KEY),
        is(trackingContext.getFilesDir().getAbsolutePath()));
  }
}
