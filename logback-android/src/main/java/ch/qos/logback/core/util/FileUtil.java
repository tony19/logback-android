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

import java.io.File;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.spi.ContextAwareBase;

import java.io.*;

public class FileUtil extends ContextAwareBase {

  public FileUtil(Context context) {
    setContext(context);
  }

  /**
   * Creates the parent directories of a file. If parent directories not
   * specified in file's path, then nothing is done and this returns
   * gracefully.
   *
   * @param file file whose parent directories (if any) should be created
   * @return {@code true} if either no parents were specified, or if all
   * parent directories were created successfully; {@code false} otherwise
   */
  static public boolean createMissingParentDirectories(File file) {
    File parent = file.getParentFile();
    if (parent == null) {
      // Parent directory not specified, therefore it's a request to
      // create nothing. Done! ;)
      return true;
    }
    // File.mkdirs() creates the parent directories only if they don't
    // already exist; and it's okay if they do.
    parent.mkdirs();
    return parent.exists();
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
    if (prefix != null && !OptionHelper.isEmpty(prefix.trim()) && !new File(path).isAbsolute()) {
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
