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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests the {@link CommonPathUtil} class
 *
 * @author Anthony Trinh
 */
public class CommonPathUtilTest {

  @Test
  public void testGetMountedExternalStorageDirectoryPathIsNotEmpty() {
    assertEquals("/mnt/sdcard", CommonPathUtil.getMountedExternalStorageDirectoryPath());
  }

  @Test
  public void testGetExternalStorageDirectoryPathIsNotEmpty() {
    assertEquals("/sdcard", CommonPathUtil.getExternalStorageDirectoryPath());
  }

  @Test
  public void testGetAssetsDirectoryPathIsNotEmpty() {
    assertEquals("assets", CommonPathUtil.getAssetsDirectoryPath());
  }
}
