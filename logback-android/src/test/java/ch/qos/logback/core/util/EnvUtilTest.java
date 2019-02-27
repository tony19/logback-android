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
package ch.qos.logback.core.util;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Patrick Reinhart
 */
public class EnvUtilTest {
    @Mock
    private String savedVersion = System.getProperty("java.version");

    @After
    public void tearDown() {
        System.setProperty("java.version", savedVersion);
    }

    @Test
    public void testJava1_4() {
        System.setProperty("java.version", "1.4.xx");

        assertFalse(EnvUtil.isJDK5());
        assertFalse(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava1_5() {
        System.setProperty("java.version", "1.5");

        assertTrue(EnvUtil.isJDK5());
        assertFalse(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava1_5_x() {
        System.setProperty("java.version", "1.5.xx");

        assertTrue(EnvUtil.isJDK5());
        assertFalse(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava1_6() {
        System.setProperty("java.version", "1.6.xx");

        assertTrue(EnvUtil.isJDK5());
        assertFalse(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava1_7() {
        System.setProperty("java.version", "1.7.xx");

        assertTrue(EnvUtil.isJDK5());
        assertTrue(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava1_8() {
        System.setProperty("java.version", "1.8.xx");

        assertTrue(EnvUtil.isJDK5());
        assertTrue(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava9() {
        System.setProperty("java.version", "9");

        assertTrue(EnvUtil.isJDK5());
        assertTrue(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava9_1() {
        System.setProperty("java.version", "9.xx");

        assertTrue(EnvUtil.isJDK5());
        assertTrue(EnvUtil.isJDK7OrHigher());
    }

    @Test
    public void testJava10() {
        System.setProperty("java.version", "10.xx");

        assertTrue(EnvUtil.isJDK5());
        assertTrue(EnvUtil.isJDK7OrHigher());
    }
}
