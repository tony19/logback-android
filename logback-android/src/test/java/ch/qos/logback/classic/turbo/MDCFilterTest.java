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
package ch.qos.logback.classic.turbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.testUtil.RandomUtil;

public class MDCFilterTest {

    int diff = RandomUtil.getPositiveInt();
    String key = "myKey" + diff;
    String value = "val" + diff;
    private MDCFilter filter;

    @Before
    public void setUp() {
        filter = new MDCFilter();
        filter.setOnMatch("ACCEPT");
        filter.setOnMismatch("DENY");
        filter.setMDCKey(key);
        filter.setValue(value);
        MDC.clear();
    }

    @After
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void smoke() {
        filter.start();
        MDC.put(key, "other" + diff);
        assertEquals(FilterReply.DENY, filter.decide(null, null, null, null, null, null));
        MDC.put(key, null);
        assertEquals(FilterReply.DENY, filter.decide(null, null, null, null, null, null));
        MDC.put(key, value);
        assertEquals(FilterReply.ACCEPT, filter.decide(null, null, null, null, null, null));
    }

    @Test
    public void testNoValueOption() {
        filter.setValue(null);
        filter.start();
        assertFalse(filter.isStarted());
        MDC.put(key, null);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, null, null, null, null, null));
        MDC.put(key, value);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, null, null, null, null, null));
    }

    @Test
    public void testNoMDCKeyOption() {
        filter.setMDCKey(null);
        filter.start();
        assertFalse(filter.isStarted());
        MDC.put(key, null);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, null, null, null, null, null));
        MDC.put(key, value);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, null, null, null, null, null));
    }
}
