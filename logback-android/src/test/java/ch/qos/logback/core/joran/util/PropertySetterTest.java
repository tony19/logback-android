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
package ch.qos.logback.core.joran.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.joran.spi.DefaultNestedComponentRegistry;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;
import ch.qos.logback.core.util.AggregationType;
import ch.qos.logback.core.util.PropertySetterException;

public class PropertySetterTest {

  DefaultNestedComponentRegistry defaultComponentRegistry = new DefaultNestedComponentRegistry();

  Context context = new ContextBase();
  House house = new House();
  PropertySetter setter = new PropertySetter(house);
 
  
  @Before
  public void setUp() {
    setter.setContext(context);
  }
  
  @Test
  public void testCanAggregateComponent() {
    assertEquals(AggregationType.AS_COMPLEX_PROPERTY, setter
        .computeAggregationType("door"));

    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("count"));
    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("Count"));

    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("name"));
    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("Name"));

    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("Duration"));
    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("fs"));

    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("open"));
    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("Open"));

    assertEquals(AggregationType.AS_COMPLEX_PROPERTY_COLLECTION, setter
        .computeAggregationType("Window"));
    assertEquals(AggregationType.AS_BASIC_PROPERTY_COLLECTION, setter
        .computeAggregationType("adjective"));

    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("filterReply"));
    assertEquals(AggregationType.AS_BASIC_PROPERTY, setter
        .computeAggregationType("houseColor"));

    System.out.println();
  }

  @Test
  public void testSetProperty() {
    {
      House house = new House();
      PropertySetter setter = new PropertySetter(house);
      setter.setProperty("count", "10");
      setter.setProperty("temperature", "33.1");
      
      setter.setProperty("name", "jack");
      setter.setProperty("open", "true");

      assertEquals(10, house.getCount());
      assertEquals(33.1d, (double) house.getTemperature(), 0.01);
      assertEquals("jack", house.getName());
      assertTrue(house.isOpen());
    }

    {
      House house = new House();
      PropertySetter setter = new PropertySetter(house);
      setter.setProperty("Count", "10");
      setter.setProperty("Name", "jack");
      setter.setProperty("Open", "true");

      assertEquals(10, house.getCount());
      assertEquals("jack", house.getName());
      assertTrue(house.isOpen());
    }
  }

  @Test
  public void testSetCamelProperty() {
    setter.setProperty("camelCase", "trot");
    assertEquals("trot", house.getCamelCase());

    setter.setProperty("camelCase", "gh");
    assertEquals("gh", house.getCamelCase());
  }

  @Test
  public void testSetComplexProperty() {
    Door door = new Door();
    setter.setComplexProperty("door", door);
    assertEquals(door, house.getDoor());
  }

  @Test
  public void testgetClassNameViaImplicitRules() {
    Class<?> compClass = setter.getClassNameViaImplicitRules("door",
        AggregationType.AS_COMPLEX_PROPERTY, defaultComponentRegistry);
    assertEquals(Door.class, compClass);
  }

  @Test
  public void testgetComplexPropertyColleClassNameViaImplicitRules() {
    Class<?> compClass = setter.getClassNameViaImplicitRules("window",
        AggregationType.AS_COMPLEX_PROPERTY_COLLECTION,
        defaultComponentRegistry);
    assertEquals(Window.class, compClass);
  }

  @Test
  public void testPropertyCollection() {
    setter.addBasicProperty("adjective", "nice");
    setter.addBasicProperty("adjective", "big");

    assertEquals(2, house.adjectiveList.size());
    assertEquals("nice", house.adjectiveList.get(0));
    assertEquals("big", house.adjectiveList.get(1));
  }

  @Test
  public void testComplexCollection() {
    Window w1 = new Window();
    w1.handle = 10;
    Window w2 = new Window();
    w2.handle = 20;

    setter.addComplexProperty("window", w1);
    setter.addComplexProperty("window", w2);
    assertEquals(2, house.windowList.size());
    assertEquals(10, house.windowList.get(0).handle);
    assertEquals(20, house.windowList.get(1).handle);
  }

  @Test
  public void testSetComplexWithCamelCaseName() {
    SwimmingPool pool = new SwimmingPoolImpl();
    setter.setComplexProperty("swimmingPool", pool);
    assertEquals(pool, house.getSwimmingPool());
  }

  @Test
  public void testDuration() {
    setter.setProperty("duration", "1.4 seconds");
    assertEquals(1400, house.getDuration().getMilliseconds());
  }

  @Test
  public void testFileSize() {
    setter.setProperty("fs", "2 kb");
    assertEquals(2 * 1024, house.getFs().getSize());
  }

  @Test
  public void testFilterReply() {
    // test case reproducing bug #52
    setter.setProperty("filterReply", "ACCEPT");
    assertEquals(FilterReply.ACCEPT, house.getFilterReply());
  }

  @Test
  public void testEnum() {
    setter.setProperty("houseColor", "BLUE");
    assertEquals(HouseColor.BLUE, house.getHouseColor());
  }

  @Test
  public void testDefaultClassAnnonation() {
    Method relevantMethod = setter.getRelevantMethod("SwimmingPool",
        AggregationType.AS_COMPLEX_PROPERTY);
    assertNotNull(relevantMethod);
    Class<?> spClass = setter.getDefaultClassNameByAnnonation("SwimmingPool",
        relevantMethod);
    assertEquals(SwimmingPoolImpl.class, spClass);

    Class<?> classViaImplicitRules = setter.getClassNameViaImplicitRules(
        "SwimmingPool", AggregationType.AS_COMPLEX_PROPERTY,
        defaultComponentRegistry);
    assertEquals(SwimmingPoolImpl.class, classViaImplicitRules);
  }
  
  @Test
  public void testDefaultClassAnnotationForLists() {
    Method relevantMethod = setter.getRelevantMethod("LargeSwimmingPool",
        AggregationType.AS_COMPLEX_PROPERTY_COLLECTION);
    assertNotNull(relevantMethod);
    Class<?> spClass = setter.getDefaultClassNameByAnnonation("LargeSwimmingPool",
        relevantMethod);
    assertEquals(LargeSwimmingPoolImpl.class, spClass);

    Class<?> classViaImplicitRules = setter.getClassNameViaImplicitRules(
        "LargeSwimmingPool", AggregationType.AS_COMPLEX_PROPERTY_COLLECTION,
        defaultComponentRegistry);
    assertEquals(LargeSwimmingPoolImpl.class, classViaImplicitRules);
  }
  
  @Test
  public void charset() {
    setter.setProperty("charset", "UTF-8");
    assertEquals(Charset.forName("UTF-8"), house.getCharset());
    
    house.setCharset(null);
    setter.setProperty("charset", "UTF");
    assertNull(house.getCharset());

    StatusChecker checker = new StatusChecker(context);
    checker.asssertContainsException(UnsupportedCharsetException.class);
  }

  // ---------------------------------------------------------------------------
  // Fixtures for the tests below. Every method name is unique, so that the
  // name-based lookups of PropertySetter are unambiguous.

  public static class Gizmo {
    String label;
    String readOnly = "fixed";
    int size;
    final List<Object> things = new ArrayList<Object>();
    final List<Integer> numbers = new ArrayList<Integer>();
    final List<String> hooks = new ArrayList<String>();

    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }

    public String getReadOnly() {
      return readOnly;
    }

    public int getSize() {
      return size;
    }

    public void setSize(int size) {
      this.size = size;
    }

    public void setExploding(String value) {
      throw new IllegalStateException("exploding setter");
    }

    // an adder without parameter, next to a usable setter of the same name
    public void addHook() {
      hooks.add("adder");
    }

    public void setHook(String hook) {
      hooks.add(hook);
    }

    public void addPair(String first, String second) {
      things.add(first + second);
    }

    public void addThing(Object thing) {
      things.add(thing);
    }

    public void addNumber(int number) {
      numbers.add(number);
    }

    public void addFailure(String value) {
      throw new IllegalStateException("failing adder");
    }

    public void setPart(Part part) {
    }

    public void setAbstractPart(AbstractPart part) {
    }

    public void setHiddenPart(HiddenPart part) {
    }

    public void setArgPart(ArgPart part) {
    }

    public void setFaultyPart(FaultyPart part) {
    }

    public void setConcretePart(ConcretePart part) {
    }
  }

  public interface Part {
  }

  public static abstract class AbstractPart {
    public AbstractPart() {
    }
  }

  public static class HiddenPart {
    private HiddenPart() {
    }
  }

  public static class ArgPart {
    public ArgPart(String arg) {
    }
  }

  public static class FaultyPart {
    public FaultyPart() {
      throw new IllegalStateException("faulty constructor");
    }
  }

  public static class ConcretePart {
  }

  private static final String GIZMO = Gizmo.class.getName();

  Gizmo gizmo = new Gizmo();

  private PropertySetter gizmoSetter() {
    PropertySetter ps = new PropertySetter(gizmo);
    ps.setContext(context);
    return ps;
  }

  private static Method gizmoMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
    return Gizmo.class.getMethod(name, parameterTypes);
  }

  private List<Status> statuses() {
    return context.getStatusManager().getCopyOfStatusList();
  }

  private Status statusWithMessage(String message) {
    for (Status s : statuses()) {
      if (message.equals(s.getMessage())) {
        return s;
      }
    }
    fail("no status with message [" + message + "] in " + statuses());
    return null;
  }

  private void assertNoStatusStartingWith(String prefix) {
    for (Status s : statuses()) {
      if (s.getMessage().startsWith(prefix)) {
        fail("unexpected status " + s);
      }
    }
  }

  @Test
  public void setPropertyIgnoresNullValue() {
    gizmo.label = "before";
    gizmoSetter().setProperty("label", null);
    assertEquals("before", gizmo.label);
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void setPropertyWarnsAboutUnknownProperty() {
    gizmoSetter().setProperty("NoSuchThing", "x");
    Status s = statusWithMessage("No such property [noSuchThing] in " + GIZMO + ".");
    assertEquals(Status.WARN, s.getLevel());
    assertEquals(1, statuses().size());
  }

  @Test
  public void setPropertyWarnsWhenPropertyHasNoSetter() {
    gizmoSetter().setProperty("readOnly", "x");
    assertEquals("fixed", gizmo.readOnly);
    Status s = statusWithMessage("Failed to set property [readOnly] to value \"x\". ");
    assertEquals(Status.WARN, s.getLevel());
    assertTrue(s.getThrowable() instanceof PropertySetterException);
    assertEquals("No setter for property [readOnly].", s.getThrowable().getMessage());
  }

  @Test
  public void setPropertyWarnsWhenValueCannotBeConverted() {
    gizmoSetter().setProperty("size", "abc");
    assertEquals(0, gizmo.size);
    Status s = statusWithMessage("Failed to set property [size] to value \"abc\". ");
    assertEquals(Status.WARN, s.getLevel());
    Throwable t = s.getThrowable();
    assertTrue(t instanceof PropertySetterException);
    assertEquals("Conversion to type [int] failed. ", t.getMessage());
    assertTrue(t.getCause() instanceof NumberFormatException);
  }

  @Test
  public void setPropertyWarnsWhenSetterThrows() {
    gizmoSetter().setProperty("exploding", "x");
    Status s = statusWithMessage("Failed to set property [exploding] to value \"x\". ");
    assertEquals(Status.WARN, s.getLevel());
    Throwable t = s.getThrowable();
    assertTrue(t instanceof PropertySetterException);
    assertTrue(t.getCause() instanceof InvocationTargetException);
    assertEquals("exploding setter", t.getCause().getCause().getMessage());
  }

  @Test
  public void setPropertyRejectsWriteMethodWithoutExactlyOneParameter() throws Exception {
    final PropertySetter ps = gizmoSetter();
    final PropertyDescriptor pd = new PropertyDescriptor("pair");
    pd.setWriteMethod(gizmoMethod("addPair", String.class, String.class));
    PropertySetterException e = assertThrows(PropertySetterException.class,
        () -> ps.setProperty(pd, "pair", "x"));
    assertEquals("#params for setter != 1", e.getMessage());
    assertTrue(gizmo.things.isEmpty());
  }

  @Test
  public void computeAggregationTypeIsNotFoundForAdderWithoutParameter() {
    // the parameterless addHook() takes precedence over the usable setHook(String)
    assertEquals(AggregationType.NOT_FOUND, gizmoSetter().computeAggregationType("hook"));
    assertEquals(AggregationType.AS_BASIC_PROPERTY, gizmoSetter().computeAggregationType("label"));
  }

  @Test
  public void getObjAndGetObjClassDescribeTheTarget() {
    PropertySetter ps = gizmoSetter();
    assertSame(gizmo, ps.getObj());
    assertSame(Gizmo.class, ps.getObjClass());
  }

  @Test
  public void addComplexPropertyReportsMissingAdder() {
    gizmoSetter().addComplexProperty("missing", new Object());
    Status s = statusWithMessage("Could not find method [addmissing] in class [" + GIZMO + "].");
    assertEquals(Status.ERROR, s.getLevel());
    assertEquals(1, statuses().size());
  }

  @Test
  public void addComplexPropertyRejectsComponentOfWrongType() {
    setter.addComplexProperty("window", new Door());
    assertTrue(house.windowList.isEmpty());
    List<Status> list = statuses();
    assertEquals(4, list.size());
    assertEquals("A \"" + Door.class.getName() + "\" object is not assignable to a \""
        + Window.class.getName() + "\" variable.", list.get(0).getMessage());
    assertEquals("The class \"" + Window.class.getName() + "\" was loaded by ", list.get(1).getMessage());
    assertEquals("[" + Window.class.getClassLoader() + "] whereas object of type ", list.get(2).getMessage());
    assertEquals("\"" + Door.class.getName() + "\" was loaded by [" + Door.class.getClassLoader() + "].",
        list.get(3).getMessage());
    for (Status s : list) {
      assertEquals(Status.ERROR, s.getLevel());
    }
  }

  @Test
  public void addComplexPropertyRejectsAdderWithWrongParameterCount() {
    gizmoSetter().addComplexProperty("pair", "x");
    assertTrue(gizmo.things.isEmpty());
    Status s = statusWithMessage("Wrong number of parameters in setter method for property [pair] in " + GIZMO);
    assertEquals(Status.ERROR, s.getLevel());
    assertEquals(1, statuses().size());
  }

  @Test
  public void addComplexPropertyReportsExceptionThrownByAdder() {
    gizmoSetter().addComplexProperty("failure", "x");
    Status s = statusWithMessage("Could not invoke method addFailure in class " + GIZMO
        + " with parameter of type java.lang.String");
    assertEquals(Status.ERROR, s.getLevel());
    assertTrue(s.getThrowable() instanceof InvocationTargetException);
    assertEquals("failing adder", s.getThrowable().getCause().getMessage());
  }

  @Test
  public void addBasicPropertyIgnoresNullValue() {
    setter.addBasicProperty("adjective", null);
    assertTrue(house.adjectiveList.isEmpty());
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void addBasicPropertyReportsMissingAdder() {
    gizmoSetter().addBasicProperty("missing", "x");
    Status s = statusWithMessage("No adder for property [Missing].");
    assertEquals(Status.ERROR, s.getLevel());
    assertEquals(1, statuses().size());
  }

  @Test
  public void addBasicPropertyReportsConversionFailure() {
    gizmoSetter().addBasicProperty("number", "abc");
    assertTrue(gizmo.numbers.isEmpty());
    Status s = statusWithMessage("Conversion to type [int] failed. ");
    assertEquals(Status.ERROR, s.getLevel());
    assertTrue(s.getThrowable() instanceof NumberFormatException);
    assertNoStatusStartingWith("Could not invoke");
  }

  @Test
  public void addBasicPropertySkipsAdderWhenValueCannotBeConverted() {
    // there is no conversion from String to Object, so addThing(Object) is not called
    gizmoSetter().addBasicProperty("thing", "x");
    assertTrue(gizmo.things.isEmpty());
    assertTrue(statuses().isEmpty());
  }

  @Test
  public void setComplexPropertyWarnsAboutUnknownProperty() {
    gizmoSetter().setComplexProperty("missing", new Object());
    Status s = statusWithMessage("Could not find PropertyDescriptor for [missing] in " + GIZMO);
    assertEquals(Status.WARN, s.getLevel());
    assertEquals(1, statuses().size());
  }

  @Test
  public void setComplexPropertyWarnsWhenPropertyHasNoSetter() {
    gizmoSetter().setComplexProperty("readOnly", "x");
    assertEquals("fixed", gizmo.readOnly);
    Status s = statusWithMessage("Not setter method for property [readOnly] in " + GIZMO);
    assertEquals(Status.WARN, s.getLevel());
    assertEquals(1, statuses().size());
  }

  @Test
  public void setComplexPropertyRejectsComponentOfWrongType() {
    setter.setComplexProperty("door", new Window());
    assertNull(house.getDoor());
    List<Status> list = statuses();
    assertEquals(4, list.size());
    assertEquals("A \"" + Window.class.getName() + "\" object is not assignable to a \""
        + Door.class.getName() + "\" variable.", list.get(0).getMessage());
  }

  @Test
  public void setComplexPropertyReportsFailureToInvokeSetter() {
    final RuntimeException failure = new RuntimeException("invocation failed");
    PropertySetter ps = new PropertySetter(house) {
      @Override
      void invokeMethodWithSingleParameterOnThisObject(Method method, Object parameter) {
        throw failure;
      }
    };
    ps.setContext(context);
    ps.setComplexProperty("door", new Door());
    assertNull(house.getDoor());
    Status s = statusWithMessage("Could not set component " + house + " for parent component " + house);
    assertEquals(Status.ERROR, s.getLevel());
    assertSame(failure, s.getThrowable());
  }

  @Test
  public void getRelevantMethodRejectsNonComplexAggregationType() {
    IllegalStateException e = assertThrows(IllegalStateException.class,
        () -> setter.getRelevantMethod("name", AggregationType.AS_BASIC_PROPERTY));
    assertEquals("AS_BASIC_PROPERTY not allowed here", e.getMessage());
  }

  @Test
  public void annotationOfMissingMethodIsNull() {
    assertNull(setter.getAnnotation(DefaultClass.class, null));
    assertNull(setter.getDefaultClassNameByAnnonation("swimmingPool", null));
  }

  @Test
  public void getByConcreteTypeIsNullWithoutSingleParameter() throws Exception {
    PropertySetter ps = gizmoSetter();
    assertNull(ps.getByConcreteType(null));
    assertNull(ps.getByConcreteType(gizmoMethod("addHook")));
    assertNull(ps.getByConcreteType(gizmoMethod("addPair", String.class, String.class)));
  }

  @Test
  public void getByConcreteTypeIsNullForParameterTypeThatCannotBeInstantiated() throws Exception {
    PropertySetter ps = gizmoSetter();
    assertNull(ps.getByConcreteType(gizmoMethod("setPart", Part.class)));
    assertNull(ps.getByConcreteType(gizmoMethod("setAbstractPart", AbstractPart.class)));
    assertNull(ps.getByConcreteType(gizmoMethod("setHiddenPart", HiddenPart.class)));
    assertNull(ps.getByConcreteType(gizmoMethod("setArgPart", ArgPart.class)));
    assertNull(ps.getByConcreteType(gizmoMethod("setFaultyPart", FaultyPart.class)));
    assertEquals(ConcretePart.class, ps.getByConcreteType(gizmoMethod("setConcretePart", ConcretePart.class)));
  }

  @Test
  public void getClassNameViaImplicitRulesIsNullWhenNoRuleApplies() {
    PropertySetter ps = gizmoSetter();
    // no such property
    assertNull(ps.getClassNameViaImplicitRules("missing", AggregationType.AS_COMPLEX_PROPERTY,
        defaultComponentRegistry));
    // an interface, without @DefaultClass
    assertNull(ps.getClassNameViaImplicitRules("part", AggregationType.AS_COMPLEX_PROPERTY,
        defaultComponentRegistry));
    assertEquals(ConcretePart.class, ps.getClassNameViaImplicitRules("concretePart",
        AggregationType.AS_COMPLEX_PROPERTY, defaultComponentRegistry));
  }

  @Test
  public void getClassNameViaImplicitRulesPrefersRegisteredDefault() {
    DefaultNestedComponentRegistry registry = new DefaultNestedComponentRegistry();
    registry.add(Gizmo.class, "part", ConcretePart.class);
    assertEquals(ConcretePart.class, gizmoSetter().getClassNameViaImplicitRules("part",
        AggregationType.AS_COMPLEX_PROPERTY, registry));
  }

  @Test
  public void failedIntrospectionIsReportedAndLeavesNoProperties() {
    try (MockedStatic<Introspector> introspector = mockStatic(Introspector.class, CALLS_REAL_METHODS)) {
      introspector.when(() -> Introspector.getPropertyDescriptors(Gizmo.class))
          .thenThrow(new IntrospectionException("cannot introspect"));

      PropertySetter ps = gizmoSetter();
      ps.setProperty("label", "x");
      // the method descriptors were emptied too: addThing(Object) is not found
      assertEquals(AggregationType.NOT_FOUND, ps.computeAggregationType("thing"));
    }
    assertNull(gizmo.label);
    Status error = statusWithMessage("Failed to introspect " + gizmo + ": cannot introspect");
    assertEquals(Status.ERROR, error.getLevel());
    Status warn = statusWithMessage("No such property [label] in " + GIZMO + ".");
    assertEquals(Status.WARN, warn.getLevel());
    assertEquals(2, statuses().size());
  }
}

