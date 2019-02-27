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
package ch.qos.logback.classic.html;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class XHTMLEntityResolver implements EntityResolver {

  // key: public id, value: relative path to DTD file
  static Map<String, String> entityMap = new HashMap<String, String>();

  static {
    entityMap.put("-//W3C//DTD XHTML 1.0 Strict//EN",
        "/dtd/xhtml1-strict.dtd");
    entityMap.put("-//W3C//ENTITIES Latin 1 for XHTML//EN",
        "/dtd/xhtml-lat1.ent");
    entityMap.put("-//W3C//ENTITIES Symbols for XHTML//EN",
        "/dtd/xhtml-symbol.ent");
    entityMap.put("-//W3C//ENTITIES Special for XHTML//EN",
        "/dtd/xhtml-special.ent");
  }

  public InputSource resolveEntity(String publicId, String systemId) {
    //System.out.println(publicId);
    final String relativePath = (String)entityMap.get(publicId);

    if (relativePath != null) {
      Class clazz = getClass();
      InputStream in =
        clazz.getResourceAsStream(relativePath);
      if (in == null) {
        return null;
      } else {
        return new InputSource(in);
      }
    } else {
      return null;
    }
  }
}
