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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import ch.qos.logback.core.contention.RunnableWithCounterAndDone;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.util.CoreTestConstants;
import ch.qos.logback.core.testUtil.RandomUtil;

public class PrudentFileAppenderInterruptTest {

    FileAppender<Object> fa = new FileAppender<Object>();
    Context context = new ContextBase();
    int diff = RandomUtil.getPositiveInt();
    String outputDirStr = CoreTestConstants.OUTPUT_DIR_PREFIX + "resilience-" + diff + "/";
    String logfileStr = outputDirStr + "output.log";

    @Before
    public void setUp() throws InterruptedException {
        context.getStatusManager().add(new OnConsoleStatusListener());

        File outputDir = new File(outputDirStr);
        outputDir.mkdirs();

        fa.setContext(context);
        fa.setName("FILE");
        fa.setPrudent(true);
        fa.setEncoder(new EchoEncoder<Object>());
        fa.setFile(logfileStr);
        fa.start();
    }

    @Test
    public void smoke() throws InterruptedException, IOException {
        Runner runner = new Runner(fa);
        Thread t = new Thread(runner);
        t.start();

        runner.latch.await();

        fa.doAppend("hello not interrupted");

        FileReader fr = new FileReader(logfileStr);
        BufferedReader br = new BufferedReader(fr);

        int totalLines = 0;
        while (br.readLine() != null) {
            totalLines++; // In this test, the content of the file does not matter
        }
        fr.close();
        br.close();

        assertEquals("Incorrect number of logged lines", 2, totalLines);
    }

    class Runner extends RunnableWithCounterAndDone {
        FileAppender<Object> fa;
        CountDownLatch latch = new CountDownLatch(1); // Just to make sure this is executed before we log in the test
        // method

        Runner(FileAppender<Object> fa) {
            this.fa = fa;
        }

        public void run() {
            Thread.currentThread().interrupt();
            fa.doAppend("hello interrupted");
            latch.countDown();
        }
    }

}
