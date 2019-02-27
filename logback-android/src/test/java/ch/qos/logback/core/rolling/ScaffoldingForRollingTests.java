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
package ch.qos.logback.core.rolling;

import org.junit.Before;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import ch.qos.logback.core.testUtil.FileToBufferUtil;
import ch.qos.logback.core.testUtil.RandomUtil;
import ch.qos.logback.core.util.CoreTestConstants;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Scaffolding for various rolling tests. Some assumptions are made: - rollover
 * periodicity is 1 second (without precluding size based roll-over)
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public abstract class ScaffoldingForRollingTests {

  static protected final String DATE_PATTERN_WITH_SECONDS = "yyyy-MM-dd_HH_mm_ss";
  static protected final String DATE_PATTERN_BY_DAY = "yyyy-MM-dd";
  static protected final SimpleDateFormat SDF = new SimpleDateFormat(
          DATE_PATTERN_WITH_SECONDS, Locale.US);
  static {
    SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private int diff = RandomUtil.getPositiveInt();
  protected String randomOutputDir = CoreTestConstants.OUTPUT_DIR_PREFIX + diff
          + "/";
  EchoEncoder<Object> encoder = new EchoEncoder<Object>();
  Context context = new ContextBase();
  protected List<String> expectedFilenameList = new ArrayList<String>();
  protected long nextRolloverThreshold; // initialized in setUp()
  protected long currentTime; // initialized in setUp()
  private List<Future<?>> futureList = new ArrayList<Future<?>>();

  @Before
  public void setUp() throws ParseException {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    dateFormat.setTimeZone(SDF.getTimeZone());
    currentTime = dateFormat.parse("2018-07-11 12:30").getTime();
    recomputeRolloverThreshold(currentTime);
  }

  private static File[] getFilesInDirectory(String outputDirStr) {
    File outputDir = new File(outputDirStr);
    return outputDir.listFiles();
  }

  private static void fileContentCheck(File[] fileArray, int runLength,
                                      String prefix, int runStart) throws IOException {
    List<String> stringList = new ArrayList<String>();
    for (File file : fileArray) {
      FileToBufferUtil.readIntoList(file, stringList);
    }

    List<String> witnessList = new ArrayList<String>();

    for (int i = runStart; i < runLength; i++) {
      witnessList.add(prefix + i);
    }
    assertEquals(witnessList, stringList);
  }

  protected static void sortedContentCheck(String outputDirStr, int runLength,
                                        String prefix) throws IOException {
    sortedContentCheck(outputDirStr, runLength, prefix, 0);
  }

  private static void sortedContentCheck(String outputDirStr, int runLength,
                                        String prefix, int runStart) throws IOException {
    File[] fileArray = getFilesInDirectory(outputDirStr);
    sortFileArrayByName(fileArray);
    fileContentCheck(fileArray, runLength, prefix, runStart);
  }

  protected static void reverseSortedContentCheck(String outputDirStr,
                                               int runLength, String prefix) throws IOException {
    File[] fileArray = getFilesInDirectory(outputDirStr);
    reverseSortFileArrayByName(fileArray);
    fileContentCheck(fileArray, runLength, prefix, 0);
  }

  private static void sortFileArrayByName(File[] fileArray) {
    Arrays.sort(fileArray, new Comparator<File>() {
      public int compare(File o1, File o2) {
        String o1Name = o1.getName();
        String o2Name = o2.getName();
        return (o1Name.compareTo(o2Name));
      }
    });
  }

  private static void reverseSortFileArrayByName(File[] fileArray) {
    Arrays.sort(fileArray, new Comparator<File>() {
      public int compare(File o1, File o2) {
        String o1Name = o1.getName();
        String o2Name = o2.getName();
        return (o2Name.compareTo(o1Name));
      }
    });
  }

  protected static void existenceCheck(List<String> filenameList) {
    for (String filename : filenameList) {
      assertTrue("File " + filename + " does not exist", new File(filename)
              .exists());
    }
  }

  protected static int existenceCount(List<String> filenameList) {
    int existenceCounter = 0;
    for (String filename : filenameList) {
      if (new File(filename).exists()) {
        existenceCounter++;
      }
    }
    return existenceCounter;
  }

  protected String testId2FileName(String testId) {
    return randomOutputDir + testId + ".log";
  }

  // assuming rollover every second

  protected void recomputeRolloverThreshold(long ct) {
    long delta = ct % 1000;
    nextRolloverThreshold = (ct - delta) + 1000;
  }

  protected boolean passThresholdTime(long nextRolloverThreshold) {
    return currentTime >= nextRolloverThreshold;
  }

  protected void incCurrentTime(long increment) {
    currentTime += increment;
  }

  private Date getDateOfCurrentPeriodsStart() {
    long delta = currentTime % 1000;
    return new Date(currentTime - delta);
  }

  protected Date getDateOfPreviousPeriodsStart() {
    long delta = currentTime % 1000;
    return new Date(currentTime - delta - 1000);
  }

  protected long getMillisOfCurrentPeriodsStart() {
    long delta = currentTime % 1000;
    return (currentTime - delta);
  }


  protected void addExpectedFileName_ByDate(String patternStr, long millis) {
    FileNamePattern fileNamePattern = new FileNamePattern(patternStr, context);
    String fn = fileNamePattern.convert(new Date(millis));
    expectedFilenameList.add(fn);
  }

  protected void addExpectedFileNamedIfItsTime_ByDate(String fileNamePatternStr) {
    if (passThresholdTime(nextRolloverThreshold)) {
      addExpectedFileName_ByDate(fileNamePatternStr, getMillisOfCurrentPeriodsStart());
      recomputeRolloverThreshold(currentTime);
    }
  }

  protected void addExpectedFileName_ByDate(String outputDir, String testId, Date date,
                                            boolean gzExtension) {

    String fn = outputDir + testId + "-" + SDF.format(date);
    if (gzExtension) {
      fn += ".gz";
    }
    expectedFilenameList.add(fn);
  }

  protected void addExpectedFileName_ByFileIndexCounter(String randomOutputDir, String testId, long millis,
                                                        int fileIndexCounter, String compressionSuffix) {
    String fn = randomOutputDir + testId + "-" + SDF.format(millis) + "-" + fileIndexCounter + ".txt" + compressionSuffix;
    expectedFilenameList.add(fn);
  }


  protected List<String> filterElementsInListBySuffix(String suffix) {
    List<String> zipFiles = new ArrayList<String>();
    for (String filename : expectedFilenameList) {
      if (filename.endsWith(suffix))
        zipFiles.add(filename);
    }
    return zipFiles;
  }

  protected void addExpectedFileNamedIfItsTime_ByDate(String outputDir, String testId,
                                                      boolean gzExtension) {
    if (passThresholdTime(nextRolloverThreshold)) {
      addExpectedFileName_ByDate(outputDir, testId, getDateOfCurrentPeriodsStart(),
              gzExtension);
      recomputeRolloverThreshold(currentTime);
    }
  }

  protected void massageExpectedFilesToCorresponToCurrentTarget(String fileName, boolean fileOptionIsSet) {
    int lastIndex = expectedFilenameList.size() - 1;
    String last = expectedFilenameList.remove(lastIndex);

    if (fileOptionIsSet) {
      expectedFilenameList.add(fileName);
    } else if (last.endsWith(".gz")) {
      int lastLen = last.length();
      String stem = last.substring(0, lastLen - 3);
      expectedFilenameList.add(stem);
    }
  }

  protected void zipEntryNameCheck(List<String> expectedFilenameList, String pattern) throws IOException {
    for (String filepath : expectedFilenameList) {
      checkZipEntryName(filepath, pattern);
    }
  }

  protected void checkZipEntryMatchesZipFilename(List<String> expectedFilenameList) throws IOException {
    for (String filepath : expectedFilenameList) {
      String stripped = stripStemFromZipFilename(filepath);
      checkZipEntryName(filepath, stripped);
    }
  }

  protected String stripStemFromZipFilename(String filepath) {
    File filepathAsFile = new File(filepath);
    String stem = filepathAsFile.getName();
    int stemLen = stem.length();
    return stem.substring(0, stemLen - ".zip".length());

  }

  protected void checkZipEntryName(String filepath, String pattern) throws IOException {
    System.out.println("Checking [" + filepath + "]");
    ZipFile zf = new ZipFile(filepath);
    try {
      Enumeration<? extends ZipEntry> entries = zf.entries();
      assert ((entries.hasMoreElements()));
      ZipEntry firstZipEntry = entries.nextElement();
      assert ((!entries.hasMoreElements()));
      System.out.println("Testing zip entry [" + firstZipEntry.getName() + "]");
      assertTrue(firstZipEntry.getName().matches(pattern));
    } finally {
      if (zf != null) {
        zf.close();
      }
    }
  }

  protected void add(Future<?> future) {
    if (future == null) return;
    if (!futureList.contains(future)) {
      futureList.add(future);
    }
  }

  protected void waitForJobsToComplete() {
    for (Future<?> future : futureList) {
      try {
        future.get(10, TimeUnit.SECONDS);
      } catch (Exception e) {
        throw new RuntimeException("unexpected exception while testing", e);
      }
    }

    futureList.clear();
  }
}
