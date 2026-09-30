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
import org.mockito.MockedStatic;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

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

  @Test
  public void nullMainUrlIsAcceptedAndWatchesNothing() {
    ConfigurationWatchList cwl = new ConfigurationWatchList();
    cwl.setMainURL(null);

    assertNull(cwl.getMainURL());
    assertTrue(cwl.getCopyOfFileWatchList().isEmpty());
  }

  @Test
  public void undecodableFileUrlIsReportedAsIllegalState() throws MalformedURLException {
    URL url = new File("a b.xml").toURI().toURL();
    ConfigurationWatchList cwl = new ConfigurationWatchList();
    UnsupportedEncodingException failure = new UnsupportedEncodingException("UTF-8");

    try (MockedStatic<URLDecoder> decoder = mockStatic(URLDecoder.class)) {
      decoder.when(() -> URLDecoder.decode(anyString(), anyString())).thenThrow(failure);

      IllegalStateException e = assertThrows(IllegalStateException.class, () -> cwl.convertToFile(url));
      assertSame(failure, e.getCause());
    }
  }
}
