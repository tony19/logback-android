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
package ch.qos.logback.classic.multiJVM;

import org.slf4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.testUtil.RandomUtil;

public class FileAppenderPerf {
  static String msgLong = "ABCDEGHIJKLMNOPQRSTUVWXYZabcdeghijklmnopqrstuvwxyz1234567890";

  static long LEN = 100 * 1000;
  static int DIFF = RandomUtil.getPositiveInt() % 1000;
  static String FILENAME;

  static LoggerContext buildLoggerContext(String filename, boolean safetyMode) {
    LoggerContext loggerContext = new LoggerContext();

    FileAppender<ILoggingEvent> fa = new FileAppender<ILoggingEvent>();

    PatternLayoutEncoder patternLayout = new PatternLayoutEncoder();
    patternLayout.setPattern("%5p %c - %m%n");
    patternLayout.setContext(loggerContext);
    patternLayout.start();

    fa.setEncoder(patternLayout);
    fa.setFile(filename);
    fa.setAppend(false);
    fa.setPrudent(safetyMode);
    fa.setContext(loggerContext);
    fa.start();

    ch.qos.logback.classic.Logger root = loggerContext
        .getLogger(Logger.ROOT_LOGGER_NAME);
    root.addAppender(fa);

    return loggerContext;
  }

  static void usage(String msg) {
    System.err.println(msg);
    System.err.println("Usage: java " + FileAppenderPerf.class.getName()
        + " filename");

    System.exit(1);
  }

  public static void main(String[] argv) throws Exception {
    if (argv.length > 1) {
      usage("Wrong number of arguments.");
    }

    if (argv.length == 0) {
      FILENAME = DIFF+"";
    } else {
      FILENAME = argv[0];
    }

    perfCase(false);
    perfCase(true);
  }

  static void perfCase(boolean safetyMode) throws Exception {
    LoggerContext lc = buildLoggerContext(FILENAME + "-" + safetyMode + ".log",
        safetyMode);
    Logger logger = lc.getLogger(FileAppenderPerf.class);

    long start = System.nanoTime();
    for (int i = 0; i < LEN; i++) {
      logger.debug(msgLong + " " + i);
    }
    // in microseconds
    double durationPerLog = (System.nanoTime() - start) / (LEN * 1000.0);

    lc.stop();

    System.out.println("Average duration of " + (durationPerLog)
        + " microseconds per log. Prudent mode=" + safetyMode);
    System.out.println("------------------------------------------------");
  }

}
