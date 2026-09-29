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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.event.InPlayListener;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.status.Status;

public class InterpretationContextTest {

  private final Context context = new ContextBase();
  private final Interpreter interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
  private final InterpretationContext ic = interpreter.getInterpretationContext();

  /** Counts the events it is notified of. */
  static class CountingListener implements InPlayListener {
    int count;

    @Override
    public void inPlay(SaxEvent event) {
      count++;
    }

    @Override
    public String toString() {
      return "CountingListener";
    }
  }

  @Test
  public void updateLocationInfoAppendsPositionOnlyWhenKnown() {
    assertNull(ic.getLocator());
    assertEquals("near ", ic.updateLocationInfo("near "));

    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(3);
    locator.setColumnNumber(14);
    interpreter.setDocumentLocator(locator);

    assertSame(locator, ic.getLocator());
    assertEquals("near 3:14", ic.updateLocationInfo("near "));
  }

  @Test
  public void objectStackIsExposedLive() {
    ic.pushObject("a");
    ic.pushObject("b");

    assertEquals(2, ic.getObjectStack().size());
    assertEquals("a", ic.getObject(0));
    assertEquals("b", ic.getObject(1));

    ic.getObjectStack().push("c");
    assertEquals("c", ic.peekObject());
  }

  @Test
  public void substitutionPropertyWithNullKeyOrValueIsIgnored() {
    ic.addSubstitutionProperty(null, "v");
    ic.addSubstitutionProperty("k", null);
    assertTrue(ic.getCopyOfPropertyMap().isEmpty());

    ic.addSubstitutionProperty("k", "  v  ");
    assertEquals("v", ic.getProperty("k"));
  }

  @Test
  public void substitutionPropertiesAreAddedTrimmedAndNullIsIgnored() {
    ic.addSubstitutionProperties(null);
    assertTrue(ic.getCopyOfPropertyMap().isEmpty());

    Properties props = new Properties();
    props.setProperty("a", " 1 ");
    props.setProperty("b", "2");
    ic.addSubstitutionProperties(props);

    Map<String, String> expected = new HashMap<String, String>();
    expected.put("a", "1");
    expected.put("b", "2");
    assertEquals(expected, ic.getCopyOfPropertyMap());
  }

  @Test
  public void inPlayListenerRegisteredTwiceIsWarnedAboutAndNotifiedOnce() {
    CountingListener listener = new CountingListener();
    assertTrue(ic.isListenerListEmpty());

    ic.addInPlayListener(listener);
    ic.addInPlayListener(listener);
    assertFalse(ic.isListenerListEmpty());

    List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
    assertEquals(1, statuses.size());
    assertEquals(Status.WARN, statuses.get(0).getLevel());
    assertEquals("InPlayListener CountingListener has been already registered", statuses.get(0).getMessage());

    ic.fireInPlay(null);
    assertEquals(1, listener.count);

    assertTrue(ic.removeInPlayListener(listener));
    assertTrue(ic.isListenerListEmpty());
    assertFalse(ic.removeInPlayListener(listener));
  }
}
