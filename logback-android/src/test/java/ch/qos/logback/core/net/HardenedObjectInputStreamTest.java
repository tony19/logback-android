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
package ch.qos.logback.core.net;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class HardenedObjectInputStreamTest {

    ByteArrayOutputStream bos;
    ObjectOutputStream oos;
    HardenedObjectInputStream inputStream;
    String[] whitelist = new String[] {Innocent.class.getName()};

    @Before
    public void setUp() throws Exception {
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void smoke() throws ClassNotFoundException, IOException {
        Innocent innocent = new Innocent();
        innocent.setAnInt(1);
        innocent.setAnInteger(2);
        innocent.setaString("smoke");
        Innocent back = writeAndRead(innocent);
        assertEquals(innocent, back);
    }

    private Innocent writeAndRead(Innocent innocent) throws IOException, ClassNotFoundException {
        writeObject(oos, innocent);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        inputStream = new HardenedObjectInputStream(bis, whitelist);
        Innocent fooBack = (Innocent) inputStream.readObject();
        inputStream.close();
        return fooBack;
    }

    private void writeObject(ObjectOutputStream oos, Object o) throws IOException {
        oos.writeObject(o);
        oos.flush();
        oos.close();
    }

    @Test
    public void denialOfService() throws ClassNotFoundException, IOException {
        // the depth limit relies on java.io.ObjectInputFilter (Java 9+)
        Assume.assumeTrue(hasObjectInputFilter());

        ByteArrayInputStream bis = new ByteArrayInputStream(payload());
        inputStream = new HardenedObjectInputStream(bis, whitelist);
        try {
            inputStream.readObject();
            fail("InvalidClassException expected");
        } catch (InvalidClassException e) {
        } finally {
            inputStream.close();
        }
    }

    private static boolean hasObjectInputFilter() {
        try {
            Class.forName("java.io.ObjectInputFilter");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private byte[] payload() throws IOException {
        Set<Object> root = buildEvilHashset();
        writeObject(oos, root);
        return bos.toByteArray();
    }

    // a graph that is small on the wire but takes ~2^100 steps to hash when deserialized
    private Set<Object> buildEvilHashset() {
        Set<Object> root = new HashSet<Object>();
        Set<Object> s1 = root;
        Set<Object> s2 = new HashSet<Object>();
        for (int i = 0; i < 100; i++) {
            Set<Object> t1 = new HashSet<Object>();
            Set<Object> t2 = new HashSet<Object>();
            t1.add("foo"); // make it not equal to t2
            s1.add(t1);
            s1.add(t2);
            s2.add(t1);
            s2.add(t2);
            s1 = t1;
            s2 = t2;
        }
        return root;
    }

    @Test
    public void acceptsJavaLangAndJavaUtilClassesWithoutWhitelist() throws Exception {
        List<Object> list = new ArrayList<Object>();
        list.add("text");
        list.add(Integer.valueOf(42));

        inputStream = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(list)), (String[]) null);
        try {
            assertEquals(list, inputStream.readObject());
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void rejectsClassesThatAreNotWhitelisted() throws Exception {
        inputStream = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(new Innocent())), (String[]) null);
        try {
            InvalidClassException e = assertThrows(InvalidClassException.class, () -> inputStream.readObject());
            assertEquals("Unauthorized deserialization attempt; " + Innocent.class.getName(), e.getMessage());
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void acceptsClassesOnWhitelistGivenAsList() throws Exception {
        Innocent innocent = new Innocent();
        innocent.setaString("listed");
        List<String> listedClasses = Arrays.asList("some.other.Clazz", Innocent.class.getName());

        inputStream = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(innocent)), listedClasses);
        try {
            assertEquals(innocent, inputStream.readObject());
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void acceptsClassesAddedToTheWhitelist() throws Exception {
        Innocent innocent = new Innocent();
        innocent.setaString("added");

        inputStream = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(innocent)), new String[0]);
        inputStream.addToWhitelist(Arrays.asList(Innocent.class.getName()));
        try {
            assertEquals(innocent, inputStream.readObject());
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void acceptsArraysUpToTheLimit() throws Exception {
        inputStream = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(new int[10000])), new String[] {"[I"});
        try {
            assertEquals(10000, ((int[]) inputStream.readObject()).length);
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void rejectsArraysLongerThanTheLimit() throws Exception {
        // the array limit relies on java.io.ObjectInputFilter (Java 9+)
        Assume.assumeTrue(hasObjectInputFilter());

        inputStream = new HardenedObjectInputStream(new ByteArrayInputStream(serialize(new int[10001])), new String[] {"[I"});
        try {
            InvalidClassException e = assertThrows(InvalidClassException.class, () -> inputStream.readObject());
            assertTrue(e.getMessage(), e.getMessage().contains("REJECTED"));
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void reliesOnWhitelistAloneWhereObjectInputFilterIsMissing() throws Exception {
        // as on Java 8 and Android, which have no java.io.ObjectInputFilter
        Class<?> isolatedClass = new ObjectInputFilterHidingClassLoader()
                .loadClass(HardenedObjectInputStream.class.getName());
        assertNotSame(HardenedObjectInputStream.class, isolatedClass);

        // no array limit...
        ObjectInputStream in = newIsolatedInputStream(isolatedClass, serialize(new int[10001]), "[I");
        try {
            assertEquals(10001, ((int[]) in.readObject()).length);
        } finally {
            in.close();
        }

        // ...but the whitelist still applies
        final ObjectInputStream rejecting = newIsolatedInputStream(isolatedClass, serialize(new Innocent()), "[I");
        try {
            assertThrows(InvalidClassException.class, () -> rejecting.readObject());
        } finally {
            rejecting.close();
        }
    }

    @Test
    public void failsWhenTheObjectFilterCannotBeInstalled() throws Exception {
        Assume.assumeTrue(hasObjectInputFilter());
        final byte[] bytes = serialize("anything");

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> new FilterPresettingInputStream(new ByteArrayInputStream(bytes)));

        assertEquals("Failed to initialize object filter", e.getMessage());
        assertTrue(e.getCause() instanceof InvocationTargetException);
        // ObjectInputStream allows setting a stream's filter only once
        assertTrue(e.getCause().getCause() instanceof IllegalStateException);
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(out);
        objectOut.writeObject(o);
        objectOut.close();
        return out.toByteArray();
    }

    private static ObjectInputStream newIsolatedInputStream(Class<?> isolatedClass, byte[] bytes, String... allowedClasses)
            throws Exception {
        return (ObjectInputStream) isolatedClass.getConstructor(InputStream.class, String[].class)
                .newInstance(new ByteArrayInputStream(bytes), allowedClasses);
    }

    /**
     * Defines its own copy of {@link HardenedObjectInputStream}, for which
     * {@code java.io.ObjectInputFilter} cannot be found.
     */
    private static class ObjectInputFilterHidingClassLoader extends ClassLoader {

        ObjectInputFilterHidingClassLoader() {
            super(HardenedObjectInputStreamTest.class.getClassLoader());
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals("java.io.ObjectInputFilter")) {
                throw new ClassNotFoundException(name);
            }
            if (!name.equals(HardenedObjectInputStream.class.getName())) {
                return super.loadClass(name, resolve);
            }
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                byte[] bytes = readClassFile(HardenedObjectInputStream.class);
                // same code source as the original class, so that coverage tools record it
                c = defineClass(name, bytes, 0, bytes.length, HardenedObjectInputStream.class.getProtectionDomain());
            }
            return c;
        }

        private static byte[] readClassFile(Class<?> c) throws ClassNotFoundException {
            InputStream in = c.getResourceAsStream(c.getSimpleName() + ".class");
            if (in == null) {
                throw new ClassNotFoundException(c.getName());
            }
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
                return out.toByteArray();
            } catch (IOException e) {
                throw new ClassNotFoundException(c.getName(), e);
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sets an object filter of its own while the stream header is read, i.e.
     * before {@link HardenedObjectInputStream} installs its filter.
     */
    private static class FilterPresettingInputStream extends HardenedObjectInputStream {

        FilterPresettingInputStream(InputStream in) throws IOException {
            super(in, new String[0]);
        }

        @Override
        protected void readStreamHeader() throws IOException {
            super.readStreamHeader();
            try {
                // java.io.ObjectInputFilter (Java 9+) is not part of the Android API
                Class<?> filterClass = Class.forName("java.io.ObjectInputFilter");
                Object filter = Class.forName("java.io.ObjectInputFilter$Config")
                        .getMethod("createFilter", String.class).invoke(null, "maxdepth=1");
                ObjectInputStream.class.getMethod("setObjectInputFilter", filterClass).invoke(this, filter);
            } catch (ReflectiveOperationException e) {
                throw new IOException(e);
            }
        }
    }

}
