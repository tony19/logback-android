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
package ch.qos.logback.classic.log4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 * A test for correct (well-formed, valid) log4j XML layout.
 *
 * @author Gabriel Corona
 */
public class XMLLayoutTest {

    private static final String DOCTYPE = "<!DOCTYPE log4j:eventSet SYSTEM \"http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd\">";
    private static final String NAMESPACE = "http://jakarta.apache.org/log4j/";
    private static final String DTD_URI = "http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/xml/doc-files/log4j.dtd";

    private static final String MDC_KEY = "key <&>'\"]]>";
    private static final String MDC_VALUE = "value <&>'\"]]>";

    private static final String MESSAGE = "test message, <&>'\"";

    private LoggerContext lc;
    private Logger root;
    private XMLLayout layout;

    @Before
    public void setUp() throws Exception {
        lc = new LoggerContext();
        lc.setName("default");

        layout = new XMLLayout();
        layout.setLocationInfo(true);
        layout.setContext(lc);
        layout.setProperties(true);
        layout.setLocationInfo(true);
        layout.start();

        root = lc.getLogger(Logger.ROOT_LOGGER_NAME);

    }

    @After
    public void tearDown() throws Exception {
        lc = null;
        layout = null;
        MDC.clear();
    }

    @Test
    public void testDoLayout() throws Exception {
        ILoggingEvent le = createLoggingEvent();

        String result = DOCTYPE + "<log4j:eventSet xmlns:log4j='http://jakarta.apache.org/log4j/'>";
        if (layout.getFileHeader() != null) {
            result += layout.getFileHeader();
        }
        if (layout.getPresentationHeader() != null) {
            result += layout.getPresentationHeader();
        }
        result += layout.doLayout(le);
        if (layout.getPresentationFooter() != null) {
            result += layout.getPresentationFooter();
        }
        if (layout.getFileFooter() != null) {
            result += layout.getFileFooter();
        }
        result += "</log4j:eventSet>";

        Document document = parse(result);

        XPath xpath = this.newXPath();

        // Test log4j:event:
        NodeList eventNodes = (NodeList) xpath.compile("//log4j:event").evaluate(document, XPathConstants.NODESET);
        Assert.assertEquals(1, eventNodes.getLength());

        // Test log4g:message:
        Assert.assertEquals(MESSAGE, xpath.compile("//log4j:message").evaluate(document, XPathConstants.STRING));

        // Test log4j:data:
        NodeList dataNodes = (NodeList) xpath.compile("//log4j:data").evaluate(document, XPathConstants.NODESET);
        boolean foundMdc = false;
        for (int i = 0; i != dataNodes.getLength(); ++i) {
            Node dataNode = dataNodes.item(i);
            if (dataNode.getAttributes().getNamedItem("name").getNodeValue().equals(MDC_KEY)) {
                foundMdc = true;
                Assert.assertEquals(MDC_VALUE, dataNode.getAttributes().getNamedItem("value").getNodeValue());
                break;
            }
        }
        Assert.assertTrue(foundMdc);
    }

    /**
     * Create a XPath instance with xmlns:log4j="http://jakarta.apache.org/log4j/"
     *
     * @return XPath instance with log4 namespace
     */
    private XPath newXPath() {
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();

        xpath.setNamespaceContext(new NamespaceContext() {
            @SuppressWarnings("rawtypes")
            public Iterator getPrefixes(String namespaceURI) {
                throw new UnsupportedOperationException();
            }

            public String getPrefix(String namespaceURI) {
                throw new UnsupportedOperationException();
            }

            public String getNamespaceURI(String prefix) {
                if ("log4j".equals(prefix)) {
                    return NAMESPACE;
                } else {
                    return XMLConstants.NULL_NS_URI;
                }
            }
        });

        return xpath;
    }

    private LoggingEvent createLoggingEvent() {
        MDC.put(MDC_KEY, MDC_VALUE);
        LoggingEvent event = new LoggingEvent("com.example.XMLLayoutTest-<&>'\"]]>", root, Level.DEBUG, MESSAGE, new RuntimeException(
                "Dummy exception: <&>'\"]]>"), null);
        event.setThreadName("Dummy thread <&>'\"");

        StackTraceElement ste1 = new StackTraceElement("c1", "m1", "f1", 1);
        StackTraceElement ste2 = new StackTraceElement("c2", "m2", "f2", 2);
        event.setCallerData(new StackTraceElement[] { ste1, ste2 });

        return event;
    }

    /**
     * Parse and validate Log4j XML
     *
     * @param output Log4j XML
     * @return Document
     * @throws Exception
     */
    private Document parse(String output) throws Exception {

        // Lookup the DTD in test resources:
        EntityResolver resolver = new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId) {
                if (publicId == null && systemId != null && systemId.equals(DTD_URI)) {
                    final String path = "/dtd/log4j.dtd";
                    InputStream in = this.getClass().getResourceAsStream(path);
                    return in != null ? new InputSource(in) : null;
                } else {
                    throw new RuntimeException("Not found");
                }
            }
        };

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(resolver);

        return builder.parse(new ByteArrayInputStream(output.getBytes("UTF-8")));
    }

}
