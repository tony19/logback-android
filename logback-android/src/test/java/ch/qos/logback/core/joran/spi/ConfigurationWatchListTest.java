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

import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static junit.framework.Assert.assertEquals;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class ConfigurationWatchListTest {

  @Test
  // See http://jira.qos.ch/browse/LBCORE-119
  public void fileToURLAndBack() throws MalformedURLException {
    File file = new File("a b.xml");
    URL url = file.toURI().toURL();
    ConfigurationWatchList cwl = new ConfigurationWatchList();
    File back = cwl.convertToFile(url);
    assertEquals(file.getName(), back.getName());
  }
}
