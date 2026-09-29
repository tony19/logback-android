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

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * HardenedObjectInputStream restricts the set of classes that can be deserialized to a set of
 * explicitly whitelisted classes. This prevents certain type of attacks from being successful.
 * 
 * <p>It is assumed that classes in the "java.lang" and  "java.util" packages are
 * always authorized.</p>
 *
 * @author Ceki G&uuml;lc&uuml;
 * @since 1.2.0
 */
public class HardenedObjectInputStream extends ObjectInputStream {

    final List<String> whitelistedClassNames;
    final static String[] JAVA_PACKAGES = new String[] { "java.lang", "java.util" };
    final private static int DEPTH_LIMIT = 16;
    final private static int ARRAY_LIMIT = 10000;

    public HardenedObjectInputStream(InputStream in, String[] whilelist) throws IOException {
        super(in);
        initObjectFilter();

        this.whitelistedClassNames = new ArrayList<String>();
        if (whilelist != null) {
            for (int i = 0; i < whilelist.length; i++) {
                this.whitelistedClassNames.add(whilelist[i]);
            }
        }
    }

    public HardenedObjectInputStream(InputStream in, List<String> whitelist) throws IOException {
        super(in);
        initObjectFilter();
        this.whitelistedClassNames = new ArrayList<String>();
        this.whitelistedClassNames.addAll(whitelist);
    }

    /**
     * Limits the depth and array sizes of the object graph (CVE-2023-6378), as in upstream
     * logback 1.2.13. Equivalent to:
     * <pre>
     * setObjectInputFilter(ObjectInputFilter.Config.createFilter(
     *     "maxarray=" + ARRAY_LIMIT + ";maxdepth=" + DEPTH_LIMIT + ";"));
     * </pre>
     * {@code java.io.ObjectInputFilter} is Java 9+ and absent from Android, so it is invoked by
     * reflection. Upstream gates this on the {@code java.version} property and throws if the class
     * is missing; Android reports {@code java.version} as "0", so this checks for the class itself
     * and, where it is missing, relies on the class allowlist alone.
     */
    private void initObjectFilter() {
        final Class<?> oifClass;
        final Class<?> oifConfigClass;
        final Method setObjectInputFilterMethod;
        final Method createFilterMethod;
        try {
            oifClass = Class.forName("java.io.ObjectInputFilter");
            oifConfigClass = Class.forName("java.io.ObjectInputFilter$Config");
            setObjectInputFilterMethod = ObjectInputStream.class.getMethod("setObjectInputFilter", oifClass);
            createFilterMethod = oifConfigClass.getMethod("createFilter", String.class);
        } catch (ClassNotFoundException e) {
            return;
        } catch (NoSuchMethodException e) {
            return;
        }

        try {
            Object filter = createFilterMethod.invoke(null, "maxarray=" + ARRAY_LIMIT + ";maxdepth=" + DEPTH_LIMIT + ";");
            setObjectInputFilterMethod.invoke(this, filter);
        } catch (IllegalAccessException e) {
            // this code should be unreachable
            throw new RuntimeException("Failed to initialize object filter", e);
        } catch (InvocationTargetException e) {
            // this code should be unreachable
            throw new RuntimeException("Failed to initialize object filter", e);
        }
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass anObjectStreamClass) throws IOException, ClassNotFoundException {
        String incomingClassName = anObjectStreamClass.getName();
        if(!isWhitelisted(incomingClassName)) {
            throw new InvalidClassException("Unauthorized deserialization attempt", anObjectStreamClass.getName());
        }

        return super.resolveClass(anObjectStreamClass);
    }

    private boolean isWhitelisted(String incomingClassName) {
        for (int i = 0; i < JAVA_PACKAGES.length; i++) {
            if (incomingClassName.startsWith(JAVA_PACKAGES[i])) {
                return true;
            }
        }
        for (String whiteListed : whitelistedClassNames) {
            if (incomingClassName.equals(whiteListed)) {
                return true;
            }
        }
        return false;
    }

    protected void addToWhitelist(List<String> additionalAuthorizedClasses) {
        whitelistedClassNames.addAll(additionalAuthorizedClasses);
    }
}
