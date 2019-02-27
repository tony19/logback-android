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
package ch.qos.logback.core;

import static ch.qos.logback.core.BasicStatusManager.MAX_HEADER_COUNT;
import static ch.qos.logback.core.BasicStatusManager.TAIL_SIZE;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusListener;


public class BasicStatusManagerTest {


  BasicStatusManager bsm = new BasicStatusManager();
  Context context = new ContextBase();
  ContextAware contextAware = new ContextAwareBase();
  OnConsoleStatusListener csl = new OnConsoleStatusListener();

  @Before
  public void before() {
    contextAware.setContext(context);
    csl.setContext(context);
    bsm.clear();
  }

  @Test
  public void smoke() {
    bsm.add(new ErrorStatus("hello", this));
    assertEquals(Status.ERROR, bsm.getLevel());

    List<Status> statusList = bsm.getCopyOfStatusList();
    assertNotNull(statusList);
    assertEquals(1, statusList.size());
    assertEquals("hello", statusList.get(0).getMessage());
  }

  @Test
  public void many() {
    int margin = 300;
    int len = MAX_HEADER_COUNT + TAIL_SIZE + margin;
    for (int i = 0; i < len; i++) {
      bsm.add(new ErrorStatus("" + i, this));
    }

    List<Status> statusList = bsm.getCopyOfStatusList();
    assertNotNull(statusList);
    assertEquals(MAX_HEADER_COUNT + TAIL_SIZE, statusList.size());
    List<Status> witness = new ArrayList<Status>();
    for (int i = 0; i < MAX_HEADER_COUNT; i++) {
      witness.add(new ErrorStatus("" + i, this));
    }
    for (int i = 0; i < TAIL_SIZE; i++) {
      witness.add(new ErrorStatus("" + (MAX_HEADER_COUNT + margin + i), this));
    }
    assertEquals(witness, statusList);
  }

  @Test
  public void duplicateInstallationsOfOnConsoleListener() {
    OnConsoleStatusListener sl0 = new OnConsoleStatusListener();
    sl0.start();
    OnConsoleStatusListener sl1 = new OnConsoleStatusListener();
    sl1.start();

    assertTrue(bsm.add(sl0));

    {
      List<StatusListener> listeners = bsm.getCopyOfStatusListenerList();
      assertEquals(1, listeners.size());
    }

    assertFalse(bsm.add(sl1));
    {
      List<StatusListener> listeners = bsm.getCopyOfStatusListenerList();
      assertEquals(1, listeners.size());
    }
  }

  @Test
  public void returnsTrueForNewlyAddedConsoleListener() {
    assertTrue(bsm.addUniquely(csl, contextAware));
    assertEquals(OnConsoleStatusListener.class, bsm.getCopyOfStatusListenerList().get(0).getClass());

    List<StatusListener> statusList = bsm.getCopyOfStatusListenerList();
    assertEquals(1, statusList.size());
  }

  @Test
  public void returnsFalseWhenNoConsoleListenerAdded() {
    bsm.addUniquely(csl, contextAware);
    assertFalse(bsm.addUniquely(csl, contextAware));

    List<StatusListener> statusList = bsm.getCopyOfStatusListenerList();
    assertEquals(1, statusList.size());
  }

  @Test
  public void addsConsoleStatusListenerOnlyIfAbsent() {
    bsm.addUniquely(csl, contextAware);

    List<StatusListener> statusList = bsm.getCopyOfStatusListenerList();
    assertEquals(1, statusList.size());
    assertEquals(csl, statusList.get(0));

    bsm.addUniquely(csl, contextAware);

    statusList = bsm.getCopyOfStatusListenerList();
    assertEquals(1, statusList.size());
    assertEquals(csl, statusList.get(0));
  }
}
