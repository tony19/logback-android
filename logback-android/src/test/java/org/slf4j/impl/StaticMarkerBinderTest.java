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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

public class StaticMarkerBinderTest {

  @Test
  public void singletonIsTheSharedInstance() {
    assertNotNull(StaticMarkerBinder.SINGLETON);
    assertSame(StaticMarkerBinder.SINGLETON, StaticMarkerBinder.getSingleton());
  }

  @Test
  public void markerFactoryIsOneSharedBasicMarkerFactory() {
    IMarkerFactory factory = StaticMarkerBinder.getSingleton().getMarkerFactory();

    assertTrue(factory instanceof BasicMarkerFactory);
    assertSame(factory, StaticMarkerBinder.getSingleton().getMarkerFactory());
    Marker marker = factory.getMarker("STATIC_MARKER_BINDER_TEST");
    assertSame(marker, StaticMarkerBinder.getSingleton().getMarkerFactory()
        .getMarker("STATIC_MARKER_BINDER_TEST"));
  }

  @Test
  public void markerFactoryClassNameIsBasicMarkerFactory() {
    assertEquals("org.slf4j.helpers.BasicMarkerFactory",
        StaticMarkerBinder.getSingleton().getMarkerFactoryClassStr());
  }
}
