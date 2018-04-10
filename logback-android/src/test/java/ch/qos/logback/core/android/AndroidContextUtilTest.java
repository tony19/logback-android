/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.android;

import android.os.Environment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowEnvironment;

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
  public void getMountedExternalStorageDirectoryPath_returnsPathWhenMounted() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(),
               is(ShadowEnvironment.getExternalStorageDirectory().getAbsolutePath()));
  }

  @Test
  public void getMountedExternalStorageDirectoryPathReturnsPathWhenMountedReadOnly() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED_READ_ONLY);
    assertThat(contextUtil.getMountedExternalStorageDirectoryPath(),
            is(ShadowEnvironment.getExternalStorageDirectory().getAbsolutePath()));
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
            is(ShadowEnvironment.getExternalStorageDirectory().getAbsolutePath()));
  }

  @Test
  public void getFilesDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getFilesDirectoryPath(), endsWith("/files"));
  }

  @Test
  public void getExternalFilesDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getExternalFilesDirectoryPath(), endsWith("/external-files"));
  }

  @Test
  public void getNoBackupFilesDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getNoBackupFilesDirectoryPath(), endsWith("/no_backup"));
  }

  @Test
  public void getCacheDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getCacheDirectoryPath(), endsWith("/cache"));
  }

  @Test
  public void getExternalCacheDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getExternalCacheDirectoryPath(), endsWith("/external-cache"));
  }

  @Test
  public void getDatabaseDirectoryPathIsNotEmpty() {
    assertThat(contextUtil.getDatabaseDirectoryPath(), endsWith("/databases"));
  }

  @Test
  public void getPackageName() {
    assertThat(contextUtil.getPackageName(), is("com.github.tony19.logback.android"));
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
