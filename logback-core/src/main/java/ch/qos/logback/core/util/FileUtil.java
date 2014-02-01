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
package ch.qos.logback.core.util;

import java.io.File;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.spi.ContextAwareBase;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class FileUtil extends ContextAwareBase {

  public FileUtil(Context context) {
    setContext(context);
  }

  public static URL fileToURL(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Unexpected exception on file [" + file + "]", e);
    }
  }

  static public boolean isParentDirectoryCreationRequired(File file) {
    File parent = file.getParentFile();
    return (parent != null && !parent.exists());
  }

  static public boolean createMissingParentDirectories(File file) {
    File parent = file.getParentFile();
    if (parent == null) {
      throw new IllegalStateException(file + " should not have a null parent");
    }
    if (parent.exists()) {
      throw new IllegalStateException(file + " should not have existing parent directory");
    }
    return parent.mkdirs();
  }

  /**
   * Prepends a string to a path if the path is relative. If the path
   * is already absolute, the same path is returned (nothing changed).
   * This is useful for converting relative paths to absolute ones,
   * given the absolute directory path as a prefix.
   *
   * @param prefix string to prepend to the evaluated path if it's not
   * already absolute
   * @param path path to evaluate
   * @return path (prefixed if relative)
   */
  public static String prefixRelativePath(String prefix, String path) {
    if (prefix != null && !prefix.trim().isEmpty() && !new File(path).isAbsolute()) {
      path = prefix + "/" + path;
    }
    return path;
  }

   static final int BUF_SIZE = 32 * 1024;

   public void copy(String src, String destination) throws RolloverFailure {
     BufferedInputStream bis = null;
     BufferedOutputStream bos = null;
     try {
       bis = new BufferedInputStream(new FileInputStream(src));
       bos = new BufferedOutputStream(new FileOutputStream(destination));
       byte[] inbuf = new byte[BUF_SIZE];
       int n;

       while ((n = bis.read(inbuf)) != -1) {
         bos.write(inbuf, 0, n);
       }

       bis.close();
       bis = null;
       bos.close();
       bos = null;
     } catch (IOException ioe) {
       String msg = "Failed to copy [" + src + "] to [" + destination + "]";
       addError(msg, ioe);
       throw new RolloverFailure(msg);
     } finally {
       if (bis != null) {
         try {
           bis.close();
         } catch (IOException e) {
           // ignore
         }
       }
       if (bos != null) {
         try {
           bos.close();
         } catch (IOException e) {
           // ignore
         }
       }
     }
   }
}
