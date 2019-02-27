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
package ch.qos.logback.core.spi;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.Appender;
import ch.qos.logback.core.helpers.NOPAppender;

/**
 * This test case verifies all the methods of AppenderAttableImpl work properly.
 *
 * @author Ralph Goers
 */
public class AppenderAttachableImplTest {

  
  private AppenderAttachableImpl<TestEvent> aai;

  @Before
  public void setUp() throws Exception {
    aai = new AppenderAttachableImpl<TestEvent>();
  }

  @After
  public void tearDown() throws Exception {
    aai = null;
  }

  @Test
  public void testAddAppender() throws Exception {
    TestEvent event = new TestEvent();
    NOPAppender<TestEvent> ta = new NOPAppender<TestEvent>();
    ta.start();
    aai.addAppender(ta);
    ta = new NOPAppender<TestEvent>();
    ta.setName("test");
    ta.start();
    aai.addAppender(ta);
    int size = aai.appendLoopOnAppenders(event);
    assertTrue("Incorrect number of appenders", size == 2);
  }

  @Test
  public void testIteratorForAppenders() throws Exception {
    NOPAppender<TestEvent> ta = new NOPAppender<TestEvent>();
    ta.start();
    aai.addAppender(ta);
    NOPAppender<TestEvent> tab = new NOPAppender<TestEvent>();
    tab.setName("test");
    tab.start();
    aai.addAppender(tab);
    Iterator<Appender<TestEvent>> iter = aai.iteratorForAppenders();
    int size = 0;
    while (iter.hasNext()) {
      ++size;
      Appender<TestEvent> app = iter.next();
      assertTrue("Bad Appender", app == ta || app == tab);
    }
    assertTrue("Incorrect number of appenders", size == 2);
  }

  @Test
  public void getGetAppender() throws Exception {
    NOPAppender<TestEvent> test = new NOPAppender<TestEvent>();
    test.setName("test");
    test.start();
    aai.addAppender(test);
    
    NOPAppender<TestEvent> testOther = new NOPAppender<TestEvent>();
    testOther.setName("testOther");
    testOther.start();
    aai.addAppender(testOther);
    
    Appender<TestEvent> a = aai.getAppender("testOther");
    assertNotNull("Could not find appender", a);
    assertTrue("Wrong appender", a == testOther);
    
    a = aai.getAppender("test");
    assertNotNull("Could not find appender", a);
    assertTrue("Wrong appender", a == test);
    a = aai.getAppender("NotThere");
    assertNull("Appender was returned", a);
  }

  @Test
  public void testIsAttached() throws Exception {
    NOPAppender<TestEvent> ta = new NOPAppender<TestEvent>();
    ta.start();
    aai.addAppender(ta);
    NOPAppender<TestEvent> tab = new NOPAppender<TestEvent>();
    tab.setName("test");
    tab.start();
    aai.addAppender(tab);
    assertTrue("Appender is not attached", aai.isAttached(ta));
    assertTrue("Appender is not attached", aai.isAttached(tab));
  }

  @Test
  public void testDetachAndStopAllAppenders() throws Exception {
    NOPAppender<TestEvent> ta = new NOPAppender<TestEvent>();
    ta.start();
    aai.addAppender(ta);
    NOPAppender<TestEvent> tab = new NOPAppender<TestEvent>();
    tab.setName("test");
    tab.start();
    aai.addAppender(tab);
    assertTrue("Appender was not started", tab.isStarted());
    aai.detachAndStopAllAppenders();
    assertNull("Appender was not removed", aai.getAppender("test"));
    assertFalse("Appender was not stopped", tab.isStarted());
  }

  @Test
  public void testDetachAppender() throws Exception {
    NOPAppender<TestEvent> ta = new NOPAppender<TestEvent>();
    ta.start();
    aai.addAppender(ta);
    NOPAppender<TestEvent> tab = new NOPAppender<TestEvent>();
    tab.setName("test");
    tab.start();
    aai.addAppender(tab);
    assertTrue("Appender not detached", aai.detachAppender(tab));
    assertNull("Appender was not removed", aai.getAppender("test"));
    assertFalse("Appender detach error", aai.detachAppender(tab));
  }

  @Test
  public void testDetachAppenderByName() throws Exception {
    NOPAppender<TestEvent> ta = new NOPAppender<TestEvent>();
    ta.setName("test1");
    ta.start();
    aai.addAppender(ta);
    NOPAppender<TestEvent> tab = new NOPAppender<TestEvent>();
    tab.setName("test");
    tab.start();
    aai.addAppender(tab);
   
    assertTrue(aai.detachAppender("test"));
    assertTrue(aai.detachAppender("test1"));
    assertFalse( aai.detachAppender("test1"));
  }

  private static class TestEvent {

  }

}

