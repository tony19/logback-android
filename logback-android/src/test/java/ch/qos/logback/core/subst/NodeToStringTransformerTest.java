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
package ch.qos.logback.core.subst;

import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class NodeToStringTransformerTest {

  static final String SYS_PROP_KEY = "NodeToStringTransformerTest.sysProp";
  static final String UNDEFINED_KEY = "NodeToStringTransformerTest.undefined";

  ContextBase propertyContainer0 = new ContextBase();
  ContextBase propertyContainer1 = new ContextBase();
  String savedSysProp;


  @Before
  public void setUp() {
    savedSysProp = System.getProperty(SYS_PROP_KEY);
    propertyContainer0.putProperty("k0", "v0");
    propertyContainer0.putProperty("zero", "0");
    propertyContainer0.putProperty("v0.jdbc.url", "http://..");
    propertyContainer0.putProperty("host", "local");

  }

  @After
  public void tearDown() {
    if (savedSysProp == null) {
      System.clearProperty(SYS_PROP_KEY);
    } else {
      System.setProperty(SYS_PROP_KEY, savedSysProp);
    }
  }

  private Node makeNode(String input) throws ScanException {
    Tokenizer tokenizer = new Tokenizer(input);
    Parser parser = new Parser(tokenizer.tokenize());
    return parser.parse();
  }

  @Test
  public void literal() throws ScanException {
    String input = "abv";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals(input, nodeToStringTransformer.transform());
  }


  void checkInputEqualsOutput(String input) throws ScanException {
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals(input, nodeToStringTransformer.transform());
  }

  @Test
  public void literalWithNestedAccolades() throws ScanException {
    checkInputEqualsOutput("%logger{35}");
    checkInputEqualsOutput("%a{35} %b{35} c");
    checkInputEqualsOutput("%replace(%msg){'\\d{14,16}', 'XXXX'}");
    checkInputEqualsOutput("TEST %d{HHmmssSSS} [%thread] %-5level %logger{36} - %msg%n");
  }


  @Test
  public void variable() throws ScanException {
    String input = "${k0}";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals("v0", nodeToStringTransformer.transform());
  }

  @Test
  public void literalVariableLiteral() throws ScanException {
    String input = "a${k0}c";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals("av0c", nodeToStringTransformer.transform());
  }

  @Test
  public void nestedVariable() throws ScanException {
    String input = "a${k${zero}}b";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals("av0b", nodeToStringTransformer.transform());
  }

  @Test
  public void LOGBACK729() throws ScanException {
    String input = "${${k0}.jdbc.url}";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals("http://..", nodeToStringTransformer.transform());
  }

  @Test
  public void LOGBACK744_withColon() throws ScanException {
    String input = "%d{HH:mm:ss.SSS} host:${host} %logger{36} - %msg%n";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    System.out.println(nodeToStringTransformer.transform());
    assertEquals("%d{HH:mm:ss.SSS} host:local %logger{36} - %msg%n", nodeToStringTransformer.transform());
  }

  @Test
  public void loneColonShouldReadLikeAnyOtherCharacter() throws ScanException {
    String input = "java:comp/env/jdbc/datasource";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals(input, nodeToStringTransformer.transform());
  }

  @Test
  public void withDefaultValue() throws ScanException {
    String input = "${k67:-b}c";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals("bc", nodeToStringTransformer.transform());
  }

  @Test
  public void defaultValueNestedAsVar() throws ScanException {
    String input = "a${k67:-x${k0}}c";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals("axv0c", nodeToStringTransformer.transform());
  }

  @Test
  public void LOGBACK_1101() throws ScanException {
    String input = "a: {y}";
    Node node = makeNode(input);
    NodeToStringTransformer nodeToStringTransformer = new NodeToStringTransformer(node, propertyContainer0);
    assertEquals("a: {y}", nodeToStringTransformer.transform());
  }

  @Test
  public void valueFromSecondPropertyContainerIsUsedWhenFirstLacksKey() throws ScanException {
    propertyContainer1.putProperty("k1", "v1");
    assertEquals("a-v1", NodeToStringTransformer.substituteVariable("a-${k1}", propertyContainer0, propertyContainer1));
  }

  @Test
  public void firstPropertyContainerTakesPrecedenceOverSecond() throws ScanException {
    propertyContainer1.putProperty("k0", "fromSecond");
    assertEquals("v0", NodeToStringTransformer.substituteVariable("${k0}", propertyContainer0, propertyContainer1));
  }

  @Test
  public void secondPropertyContainerTakesPrecedenceOverSystemProperties() throws ScanException {
    System.setProperty(SYS_PROP_KEY, "fromSystem");
    propertyContainer1.putProperty(SYS_PROP_KEY, "fromSecond");
    assertEquals("fromSecond", NodeToStringTransformer.substituteVariable("${" + SYS_PROP_KEY + "}", propertyContainer0, propertyContainer1));
  }

  @Test
  public void systemPropertyIsUsedWhenNeitherContainerHasKey() throws ScanException {
    System.setProperty(SYS_PROP_KEY, "fromSystem");
    propertyContainer1.putProperty("k1", "v1");
    assertEquals("fromSystem", NodeToStringTransformer.substituteVariable("${" + SYS_PROP_KEY + "}", propertyContainer0, propertyContainer1));
  }

  @Test
  public void environmentVariableIsUsedWhenNoContainerOrSystemPropertyHasKey() throws ScanException {
    try (MockedStatic<OptionHelper> optionHelper = Mockito.mockStatic(OptionHelper.class, Mockito.CALLS_REAL_METHODS)) {
      optionHelper.when(() -> OptionHelper.getEnv(SYS_PROP_KEY)).thenReturn("fromEnv");
      assertEquals("fromEnv", NodeToStringTransformer.substituteVariable("${" + SYS_PROP_KEY + "}", propertyContainer0, propertyContainer1));
    }
  }

  @Test
  public void systemPropertyTakesPrecedenceOverEnvironmentVariable() throws ScanException {
    System.setProperty(SYS_PROP_KEY, "fromSystem");
    try (MockedStatic<OptionHelper> optionHelper = Mockito.mockStatic(OptionHelper.class, Mockito.CALLS_REAL_METHODS)) {
      optionHelper.when(() -> OptionHelper.getEnv(SYS_PROP_KEY)).thenReturn("fromEnv");
      assertEquals("fromSystem", NodeToStringTransformer.substituteVariable("${" + SYS_PROP_KEY + "}", propertyContainer0, propertyContainer1));
    }
  }

  @Test
  public void keyMissingEverywhereIsReportedAsUndefined() throws ScanException {
    propertyContainer1.putProperty("k1", "v1");
    assertEquals(UNDEFINED_KEY + CoreConstants.UNDEFINED_PROPERTY_SUFFIX,
        NodeToStringTransformer.substituteVariable("${" + UNDEFINED_KEY + "}", propertyContainer0, propertyContainer1));
  }

  @Test
  public void circularReferenceIsReported() {
    propertyContainer0.putProperty("a", "${b}");
    propertyContainer0.putProperty("b", "${a}");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> NodeToStringTransformer.substituteVariable("${a}", propertyContainer0, null));
    assertEquals("Circular variable reference detected while parsing input [${a} --> ${b} --> ${a}]", e.getMessage());
  }

  @Test
  public void selfReferenceWithSameDefaultValueIsReportedAsCircular() {
    propertyContainer0.putProperty("self", "${self:-x}");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> NodeToStringTransformer.substituteVariable("${self:-x}", propertyContainer0, null));
    assertEquals("Circular variable reference detected while parsing input [${self} --> ${self}]", e.getMessage());
  }

  @Test
  public void selfReferenceWithOtherDefaultValueIsNotTheSameVariable() {
    // "${self:-y}" differs from "${self:-x}" by its default value only; the cycle
    // is detected one level deeper, when "${self:-y}" is met a second time.
    propertyContainer0.putProperty("self", "${self:-y}");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> NodeToStringTransformer.substituteVariable("${self:-x}", propertyContainer0, null));
    assertEquals("Circular variable reference detected while parsing input [${self} --> ${self} --> ${self}]", e.getMessage());
  }

  private boolean equalNodes(Node candidate, Node visited) throws Exception {
    Method m = NodeToStringTransformer.class.getDeclaredMethod("equalNodes", Node.class, Node.class);
    m.setAccessible(true);
    return (Boolean) m.invoke(new NodeToStringTransformer(null, propertyContainer0), candidate, visited);
  }

  private static Node literal(String s) {
    return new Node(Node.Type.LITERAL, s);
  }

  @Test
  public void cycleCheckTreatsNodesOfDifferentTypesAsDistinct() throws Exception {
    assertFalse(equalNodes(new Node(Node.Type.LITERAL, literal("k")), new Node(Node.Type.VARIABLE, literal("k"))));
  }

  @Test
  public void cycleCheckComparesPayloadAndDefaultPart() throws Exception {
    Node visited = new Node(Node.Type.VARIABLE, literal("k"), literal("d"));
    assertTrue(equalNodes(new Node(Node.Type.VARIABLE, literal("k"), literal("d")), visited));
    assertFalse(equalNodes(new Node(Node.Type.VARIABLE, literal("other"), literal("d")), visited));
    assertFalse(equalNodes(new Node(Node.Type.VARIABLE, literal("k"), literal("other")), visited));
  }

  @Test
  public void cycleCheckIgnoresFieldsTheCandidateNodeLacks() throws Exception {
    Node visited = new Node(Node.Type.VARIABLE, literal("k"), literal("d"));
    assertTrue(equalNodes(new Node(null, literal("k"), literal("d")), visited));
    assertTrue(equalNodes(new Node(Node.Type.VARIABLE, null, literal("d")), visited));
    assertTrue(equalNodes(new Node(Node.Type.VARIABLE, literal("k")), visited));
  }

  @Test
  public void cycleCheckIgnoresNextNode() throws Exception {
    Node candidate = new Node(Node.Type.VARIABLE, literal("k"));
    candidate.next = literal("tail");
    assertTrue(equalNodes(candidate, new Node(Node.Type.VARIABLE, literal("k"))));
  }
}
