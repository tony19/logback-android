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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;

import org.junit.Test;
import org.slf4j.spi.LocationAwareLogger;

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

    @Test
    public void toLevelStringAcceptsAllAndOffIgnoringCase() {
        assertSame(Level.ALL, Level.toLevel("all"));
        assertSame(Level.OFF, Level.toLevel("Off"));
        assertSame(Level.TRACE, Level.toLevel("trace", Level.ERROR));
    }

    @Test
    public void toLevelStringReturnsDefaultForNullOrUnknownInput() {
        assertSame(Level.WARN, Level.toLevel((String) null, Level.WARN));
        assertSame(Level.ERROR, Level.toLevel("VERBOSE", Level.ERROR));
        assertSame(Level.DEBUG, Level.toLevel("VERBOSE"));
        assertSame(Level.DEBUG, Level.toLevel((String) null));
    }

    @Test
    public void valueOfFollowsToLevelWithDebugAsDefault() {
        assertSame(Level.ERROR, Level.valueOf("error"));
        assertSame(Level.DEBUG, Level.valueOf("unknown"));
    }

    @Test
    public void toLevelIntMapsEveryLevelIntToItsLevel() {
        assertSame(Level.ALL, Level.toLevel(Level.ALL_INT));
        assertSame(Level.TRACE, Level.toLevel(Level.TRACE_INT));
        assertSame(Level.DEBUG, Level.toLevel(Level.DEBUG_INT, Level.OFF));
        assertSame(Level.INFO, Level.toLevel(Level.INFO_INT));
        assertSame(Level.WARN, Level.toLevel(Level.WARN_INT));
        assertSame(Level.ERROR, Level.toLevel(Level.ERROR_INT));
        assertSame(Level.OFF, Level.toLevel(Level.OFF_INT));
    }

    @Test
    public void toLevelIntReturnsDefaultForUnknownInt() {
        assertSame(Level.INFO, Level.toLevel(12345, Level.INFO));
        assertSame(Level.DEBUG, Level.toLevel(12345));
    }

    @Test
    public void toIntegerReturnsTheCachedIntegerOfEachLevel() {
        assertSame(Level.ALL_INTEGER, Level.ALL.toInteger());
        assertSame(Level.TRACE_INTEGER, Level.TRACE.toInteger());
        assertSame(Level.DEBUG_INTEGER, Level.DEBUG.toInteger());
        assertSame(Level.INFO_INTEGER, Level.INFO.toInteger());
        assertSame(Level.WARN_INTEGER, Level.WARN.toInteger());
        assertSame(Level.ERROR_INTEGER, Level.ERROR.toInteger());
        assertSame(Level.OFF_INTEGER, Level.OFF.toInteger());
        assertEquals(Integer.valueOf(Level.WARN_INT), Level.WARN.toInteger());
    }

    @Test
    public void toIntegerRejectsALevelWithAnUnknownInt() throws Exception {
        Constructor<Level> constructor = Level.class.getDeclaredConstructor(int.class, String.class);
        constructor.setAccessible(true);
        final Level bogus = constructor.newInstance(12345, "BOGUS");

        IllegalStateException e = assertThrows(IllegalStateException.class, bogus::toInteger);
        assertEquals("Level BOGUS, 12345 is unknown.", e.getMessage());
    }

    @Test
    public void toStringAndToIntExposeNameAndValue() {
        assertEquals("WARN", Level.WARN.toString());
        assertEquals(Level.WARN_INT, Level.WARN.toInt());
    }

    @Test
    public void isGreaterOrEqualComparesLevelInts() {
        assertTrue(Level.WARN.isGreaterOrEqual(Level.INFO));
        assertTrue(Level.WARN.isGreaterOrEqual(Level.WARN));
        assertFalse(Level.INFO.isGreaterOrEqual(Level.WARN));
    }

    @Test
    public void fromLocationAwareLoggerIntegerMapsEverySlf4jLevel() {
        assertSame(Level.TRACE, Level.fromLocationAwareLoggerInteger(LocationAwareLogger.TRACE_INT));
        assertSame(Level.DEBUG, Level.fromLocationAwareLoggerInteger(LocationAwareLogger.DEBUG_INT));
        assertSame(Level.INFO, Level.fromLocationAwareLoggerInteger(LocationAwareLogger.INFO_INT));
        assertSame(Level.WARN, Level.fromLocationAwareLoggerInteger(LocationAwareLogger.WARN_INT));
        assertSame(Level.ERROR, Level.fromLocationAwareLoggerInteger(LocationAwareLogger.ERROR_INT));
    }

    @Test
    public void fromLocationAwareLoggerIntegerRejectsUnknownValue() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> Level.fromLocationAwareLoggerInteger(7));
        assertEquals("7 not a valid level value", e.getMessage());
    }

    @Test
    public void toLocationAwareLoggerIntegerMapsEveryLoggableLevel() {
        assertEquals(LocationAwareLogger.TRACE_INT, Level.toLocationAwareLoggerInteger(Level.TRACE));
        assertEquals(LocationAwareLogger.DEBUG_INT, Level.toLocationAwareLoggerInteger(Level.DEBUG));
        assertEquals(LocationAwareLogger.INFO_INT, Level.toLocationAwareLoggerInteger(Level.INFO));
        assertEquals(LocationAwareLogger.WARN_INT, Level.toLocationAwareLoggerInteger(Level.WARN));
        assertEquals(LocationAwareLogger.ERROR_INT, Level.toLocationAwareLoggerInteger(Level.ERROR));
    }

    @Test
    public void toLocationAwareLoggerIntegerRejectsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> Level.toLocationAwareLoggerInteger(null));
        assertEquals("null level parameter is not admitted", e.getMessage());
    }

    @Test
    public void toLocationAwareLoggerIntegerRejectsLevelsWithoutSlf4jCounterpart() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> Level.toLocationAwareLoggerInteger(Level.OFF));
        assertEquals("OFF not a valid level value", e.getMessage());
        e = assertThrows(IllegalArgumentException.class,
            () -> Level.toLocationAwareLoggerInteger(Level.ALL));
        assertEquals("ALL not a valid level value", e.getMessage());
    }

    @Test
    public void deserializationResolvesToTheFlyweightInstance() throws Exception {
        for (Level level : new Level[] {Level.ALL, Level.TRACE, Level.INFO, Level.OFF}) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(level);
            oos.close();
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            Object back = ois.readObject();
            ois.close();
            assertSame(level, back);
        }
    }
}
