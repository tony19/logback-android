/*
 * Copyright (c) 2020 Anthony Trinh.
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
package ch.qos.logback.core.android;

import android.os.Environment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowEnvironment;

import java.io.File;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.CoreConstants;

/**
 * Tests the {@link AndroidContextUtil} class
 *
 * @author Anthony Trinh
 */
@RunWith(RobolectricTestRunner.class)
public class AndroidContextUtilTest {
  private AndroidContextUtil contextUtil;

  @Before
  public void before() {
    ShadowEnvironment.reset();
    contextUtil = new AndroidContextUtil();
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsPathWhenMounted() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(),
               is(Environment.getExternalStorageDirectory().getAbsolutePath()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsPathWhenMountedReadOnly() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED_READ_ONLY);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(),
            is(Environment.getExternalStorageDirectory().getAbsolutePath()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenRemoved() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_REMOVED);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenBadRemoval() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_BAD_REMOVAL);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenChecking() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_CHECKING);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenEjecting() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_EJECTING);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenNoFs() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_NOFS);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenUnknown() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNKNOWN);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenUnmountable() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_UNMOUNTABLE);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsNullWhenShared() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_SHARED);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(), is(nullValue()));
  }

  @Test
  public void getExternalStorageDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getExternalStorageDirectoryPath(),
            is(Environment.getExternalStorageDirectory().getAbsolutePath()));
  }

  @Test
  public void getFilesDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getFilesDirectoryPath(), endsWith(File.separator + "files"));
  }

  @Test
  public void getExternalFilesDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getExternalFilesDirectoryPath(), endsWith(File.separator + "external-files"));
  }

  @Test
  public void getNoBackupFilesDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getNoBackupFilesDirectoryPath(), endsWith(File.separator + "no_backup"));
  }

  @Test
  public void getCacheDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getCacheDirectoryPath(), endsWith(File.separator + "cache"));
  }

  @Test
  public void getExternalCacheDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getExternalCacheDirectoryPath(), endsWith(File.separator + "external-cache"));
  }

  @Test
  public void getDatabaseDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getDatabaseDirectoryPath(), endsWith(File.separator + "databases"));
  }

  @Test
  public void getPackageName() {
    assertThat(contextUtil.getPackageName(), startsWith("com.github.tony19.logback.android"));
  }

  @Test
  public void setupProperties() {
    LoggerContext loggerContext = new LoggerContext();

    assertThat(loggerContext.getProperty(CoreConstants.DATA_DIR_KEY), is(nullValue()));
    assertThat(loggerContext.getProperty(CoreConstants.EXT_DIR_KEY), is(nullValue()));
    assertThat(loggerContext.getProperty(CoreConstants.VERSION_CODE_KEY), is(nullValue()));
    assertThat(loggerContext.getProperty(CoreConstants.VERSION_NAME_KEY), is(nullValue()));
    assertThat(loggerContext.getProperty(CoreConstants.PACKAGE_NAME_KEY), is(nullValue()));

    contextUtil.setupProperties(loggerContext);

    assertThat(loggerContext.getProperty(CoreConstants.DATA_DIR_KEY), is(contextUtil.getFilesDirectoryPath()));
    assertThat(loggerContext.getProperty(CoreConstants.EXT_DIR_KEY), is(contextUtil.getMountedExternalStorageDirectoryPath()));
    assertThat(loggerContext.getProperty(CoreConstants.VERSION_CODE_KEY), is(contextUtil.getVersionCode()));
    assertThat(loggerContext.getProperty(CoreConstants.VERSION_NAME_KEY), is(contextUtil.getVersionName()));
    assertThat(loggerContext.getProperty(CoreConstants.PACKAGE_NAME_KEY), is(contextUtil.getPackageName()));
  }
}
