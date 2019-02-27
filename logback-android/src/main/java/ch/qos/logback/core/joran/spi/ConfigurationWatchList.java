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
package ch.qos.logback.core.joran.spi;


import ch.qos.logback.core.spi.ContextAwareBase;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class ConfigurationWatchList extends ContextAwareBase {

  URL mainURL;
  List<File> fileWatchList = new ArrayList<File>();
  List<Long> lastModifiedList = new ArrayList<Long>();

  public ConfigurationWatchList buildClone() {
    ConfigurationWatchList out = new ConfigurationWatchList();
    out.mainURL = this.mainURL;
    out.fileWatchList = new ArrayList<File>(this.fileWatchList);
    out.lastModifiedList = new ArrayList<Long>(this.lastModifiedList);
    return out;
  }

  public void clear() {
    this.mainURL = null;
    lastModifiedList.clear();
    fileWatchList.clear();
  }

  /**
   * The mainURL for the configuration file. Null values are allowed.
   * @param mainURL desired URL
   */
  public void setMainURL(URL mainURL) {
    // main url can be null
    this.mainURL = mainURL;
    if (mainURL != null)
      addAsFileToWatch(mainURL);
  }

  private void addAsFileToWatch(URL url) {
    File file = convertToFile(url);
    if (file != null) {
      fileWatchList.add(file);
      lastModifiedList.add(file.lastModified());
    }
  }

  public void addToWatchList(URL url) {
    addAsFileToWatch(url);
  }

  public URL getMainURL() {
    return mainURL;
  }

  public List<File> getCopyOfFileWatchList() {
    return new ArrayList<File>(fileWatchList);
  }

  public boolean changeDetected() {
    int len = fileWatchList.size();
    for (int i = 0; i < len; i++) {
      long lastModified = lastModifiedList.get(i);
      File file = fileWatchList.get(i);
      if (lastModified != file.lastModified()) {
        return true;
      }
    }
    return false;
    //return (lastModified != fileToScan.lastModified() && lastModified != SENTINEL);
  }

  @SuppressWarnings("deprecation")
  File convertToFile(URL url) {
    String protocol = url.getProtocol();
    if ("file".equals(protocol)) {
      return new File(URLDecoder.decode(url.getFile()));
    } else {
      addInfo("URL [" + url + "] is not of type file");
      return null;
    }
  }

}
