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

import android.content.ContextWrapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link AndroidContextUtil} class
 *
 * @author Anthony Trinh
 */
public class AndroidContextUtilTest {

  private AndroidContextUtil contextUtil;

  @Before
  public void before() {
    ContextWrapper context = mock(ContextWrapper.class);
    when(context.getPackageName()).thenReturn("com.github.foo");
    when(context.get)
    contextUtil = new AndroidContextUtil(context);
  }

  @Test
  public void testGetMountedExternalStorageDirectoryPathIsNotEmpty() {
    assertEquals("/mnt/sdcard", contextUtil.getMountedExternalStorageDirectoryPath());
  }

  @Test
  public void testGetExternalStorageDirectoryPathIsNotEmpty() {
    assertEquals("/sdcard", contextUtil.getExternalStorageDirectoryPath());
  }

  @Test
  public void testGetAssetsDirectoryPathIsNotEmpty() {
    assertEquals("assets", contextUtil.getAssetsDirectoryPath());
  }

  @Test
  public void testGetFilesDirectoryPathIsNotEmpty() {
    assertEquals("/data/data/android/files", contextUtil.getFilesDirectoryPath());
  }

  @Test
  public void testGetDatabaseDirectoryPathIsNotEmpty() {
    assertEquals("/data/data/android/databases", contextUtil.getDatabaseDirectoryPath());
  }
}
