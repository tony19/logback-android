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
package ch.qos.logback.core.joran;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Sanity checks for the published logback.xsd (issue #381).
 *
 * The JDK's schema compiler (Xerces) accepts some escape sequences that the
 * XML Schema spec forbids, while stricter validators (e.g. libxml2/xmllint)
 * reject the entire schema. So in addition to compiling the schema, verify
 * that every pattern facet uses only spec-legal escapes.
 */
public class LogbackSchemaTest {

  private File schemaFile;

  @Before
  public void setup() {
    // unit tests run with the module directory as the working directory;
    // the schema lives at the repository root
    schemaFile = new File("../logback.xsd");
    if (!schemaFile.isFile()) {
      schemaFile = new File("logback.xsd");
    }
    assertTrue("logback.xsd not found relative to " + new File(".").getAbsolutePath(),
        schemaFile.isFile());
  }

  @Test
  public void schemaCompiles() throws Exception {
    Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(new StreamSource(schemaFile));
    assertNotNull("schema failed to compile", schema);

    // validate a representative config, including a property reference as a
    // level value, which is what the pattern facets on line ~782 exist for
    String config =
        "<configuration xmlns='https://tony19.github.io/logback-android/xml'>" +
        "  <appender name='logcat' class='ch.qos.logback.classic.android.LogcatAppender'>" +
        "    <encoder><pattern>%msg%n</pattern></encoder>" +
        "  </appender>" +
        "  <root level='${LOG_LEVEL}'>" +
        "    <appender-ref ref='logcat'/>" +
        "  </root>" +
        "</configuration>";
    schema.newValidator().validate(new StreamSource(new StringReader(config)));
  }

  @Test
  public void patternFacetsUseOnlyLegalEscapes() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(schemaFile.toURI().toString()));

    NodeList patterns = doc.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "pattern");
    assertTrue("expected pattern facets in schema", patterns.getLength() > 0);

    List<String> errors = new ArrayList<String>();
    for (int i = 0; i < patterns.getLength(); i++) {
      String value = patterns.item(i).getAttributes().getNamedItem("value").getNodeValue();
      String error = findIllegalEscape(value);
      if (error != null) {
        errors.add("pattern \"" + value + "\": " + error);
      }
    }
    if (!errors.isEmpty()) {
      fail("illegal escape sequences in logback.xsd (rejected by strict XSD validators):\n"
          + String.join("\n", errors));
    }
  }

  /**
   * Returns a description of the first escape sequence in the given XSD regex
   * that is not permitted by https://www.w3.org/TR/xmlschema-2/#regexs
   * (SingleCharEsc, MultiCharEsc, catEsc/complEsc), or null if all are legal.
   * Note {@code \$} is NOT a legal escape: {@code $} has no special meaning in
   * XSD regexes and must appear unescaped.
   */
  private static String findIllegalEscape(String regex) {
    final String singleCharEscapes = "nrt\\|.?*+(){}-[]^";
    final String multiCharEscapes = "sSiIcCdDwW";
    for (int i = 0; i < regex.length() - 1; i++) {
      if (regex.charAt(i) != '\\') {
        continue;
      }
      char escaped = regex.charAt(++i);
      if (escaped == 'p' || escaped == 'P') {
        continue; // category escape; brace content checked by schema compiler
      }
      if (singleCharEscapes.indexOf(escaped) < 0 && multiCharEscapes.indexOf(escaped) < 0) {
        return "illegal escape sequence \\" + escaped;
      }
    }
    return null;
  }
}
