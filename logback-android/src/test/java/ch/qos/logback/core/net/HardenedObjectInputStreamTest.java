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
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
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

}
