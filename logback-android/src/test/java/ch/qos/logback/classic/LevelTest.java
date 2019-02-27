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
package ch.qos.logback.classic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LevelTest {

    @Test
    public void smoke( ) {
        assertEquals(Level.TRACE, Level.toLevel("TRACE"));
        assertEquals(Level.DEBUG, Level.toLevel("DEBUG"));
        assertEquals(Level.INFO, Level.toLevel("INFO"));
        assertEquals(Level.WARN, Level.toLevel("WARN"));
        assertEquals(Level.ERROR, Level.toLevel("ERROR"));
    }

    @Test
    public void withSpacePrefix( ) {
        assertEquals(Level.INFO, Level.toLevel("   INFO "));
    }

    @Test
    public void withSpaceSuffix( ) {
        assertEquals(Level.INFO, Level.toLevel("INFO   "));
    }

    @Test
    public void withSpaceAround( ) {
        assertEquals(Level.INFO, Level.toLevel("   INFO   "));
    }
}
