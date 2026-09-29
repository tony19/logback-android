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
package org.slf4j.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.spi.MDCAdapter;

import ch.qos.logback.classic.util.LogbackMDCAdapter;

public class StaticMDCBinderTest {

  @Test
  public void singletonIsTheSharedInstance() {
    assertNotNull(StaticMDCBinder.SINGLETON);
    assertSame(StaticMDCBinder.SINGLETON, StaticMDCBinder.getSingleton());
  }

  @Test
  public void providesAFreshLogbackMdcAdapterOnEachCall() {
    MDCAdapter first = StaticMDCBinder.getSingleton().getMDCA();
    MDCAdapter second = StaticMDCBinder.getSingleton().getMDCA();

    assertTrue(first instanceof LogbackMDCAdapter);
    assertTrue(second instanceof LogbackMDCAdapter);
    assertNotSame(first, second);
  }

  @Test
  public void adapterClassNameIsLogbackMdcAdapter() {
    assertEquals("ch.qos.logback.classic.util.LogbackMDCAdapter",
        StaticMDCBinder.getSingleton().getMDCAdapterClassStr());
  }
}
