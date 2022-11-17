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

import java.io.File;
import java.util.Date;

import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.DefaultFileProvider;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.rolling.helper.SizeAndTimeBasedArchiveRemover;
import ch.qos.logback.core.util.FileSize;

import static ch.qos.logback.core.CoreConstants.MANUAL_URL_PREFIX;

@NoAutoStart
public class SizeAndTimeBasedFNATP<E> extends
        TimeBasedFileNamingAndTriggeringPolicyBase<E> {

  enum Usage {EMBEDDED, DIRECT};

  int currentPeriodsCounter = 0;
  FileSize maxFileSize;

  static String MISSING_INT_TOKEN = "Missing integer token, that is %i, in FileNamePattern [";
  static String MISSING_DATE_TOKEN = "Missing date token, that is %d, in FileNamePattern [";

  private final Usage usage;

  public SizeAndTimeBasedFNATP() {
    this(Usage.DIRECT);
  }

  public SizeAndTimeBasedFNATP(Usage usage) {
    this.usage = usage;
  }

  @Override
  public void start() {
    // we depend on certain fields having been initialized
    // in super.start()
    super.start();

    if (usage == Usage.DIRECT) {
      addWarn(CoreConstants.SIZE_AND_TIME_BASED_FNATP_IS_DEPRECATED);
      addWarn("For more information see "+MANUAL_URL_PREFIX+"appenders.html#SizeAndTimeBasedRollingPolicy");
    }

    if (!super.isErrorFree()) {
      return;
    }

    if (maxFileSize == null) {
      addError("maxFileSize property is mandatory.");
      withErrors();
    }

    if (!validDateAndIntegerTokens()) {
      withErrors();
      return;
    }

    archiveRemover = createArchiveRemover();
    archiveRemover.setContext(context);

    // we need to get the correct value of currentPeriodsCounter.
    // usually the value is 0, unless the appender or the application
    // is stopped and restarted within the same period
    String regex = tbrp.fileNamePattern.toRegexForFixedDate(dateInCurrentPeriod);
    String stemRegex = FileFilterUtil.afterLastSlash(regex);


    computeCurrentPeriodsHighestCounterValue(stemRegex);

    if (isErrorFree()) {
      started = true;
    }
  }

  private boolean validDateAndIntegerTokens() {
    boolean inError = false;
    if (tbrp.fileNamePattern.getIntegerTokenConverter() == null) {
      inError = true;
      addError(MISSING_INT_TOKEN + tbrp.fileNamePatternStr + "]");
      addError(CoreConstants.SEE_MISSING_INTEGER_TOKEN);
    }
    if (tbrp.fileNamePattern.getPrimaryDateTokenConverter() == null) {
      inError = true;
      addError(MISSING_DATE_TOKEN + tbrp.fileNamePatternStr + "]");
    }

    return !inError;
  }

  protected ArchiveRemover createArchiveRemover() {
    return new SizeAndTimeBasedArchiveRemover(tbrp.fileNamePattern, rc, new DefaultFileProvider());
  }

  void computeCurrentPeriodsHighestCounterValue(final String stemRegex) {
    File file = new File(getCurrentPeriodsFileNameWithoutCompressionSuffix());
    File parentDir = file.getParentFile();

    File[] matchingFileArray = FileFilterUtil
            .filesInFolderMatchingStemRegex(parentDir, stemRegex);

    if (matchingFileArray == null || matchingFileArray.length == 0) {
      currentPeriodsCounter = 0;
      return;
    }
    currentPeriodsCounter = FileFilterUtil.findHighestCounter(matchingFileArray, stemRegex);

    // if parent raw file property is not null, then the next
    // counter is max  found counter+1
    if (tbrp.getParentsRawFileProperty() != null || (tbrp.compressionMode != CompressionMode.NONE)) {
      // TODO test me
      currentPeriodsCounter++;
    }
  }

  @Override
  public boolean isTriggeringEvent(File activeFile, final E event) {

    long time = getCurrentTime();
    // first check for roll-over based on time
    if (time >= nextCheck) {
      Date dateInElapsedPeriod = dateInCurrentPeriod;
      elapsedPeriodsFileName = tbrp.fileNamePatternWithoutCompSuffix.convertMultipleArguments(dateInElapsedPeriod, currentPeriodsCounter);
      currentPeriodsCounter = 0;
      setDateInCurrentPeriod(time);
      computeNextCheck();
      return true;
    }

    if (activeFile == null) {
      addWarn("activeFile == null");
      return false;
    }
    if (maxFileSize == null) {
      addWarn("maxFileSize = null");
      return false;
    }
    if (activeFile.length() >= maxFileSize.getSize()) {
      elapsedPeriodsFileName = tbrp.fileNamePatternWithoutCompSuffix.convertMultipleArguments(dateInCurrentPeriod, currentPeriodsCounter);
      currentPeriodsCounter++;
      return true;
    }

    return false;
  }

  @Override
  public String getCurrentPeriodsFileNameWithoutCompressionSuffix() {
    return tbrp.fileNamePatternWithoutCompSuffix.convertMultipleArguments(dateInCurrentPeriod, currentPeriodsCounter);
  }

  public void setMaxFileSize(FileSize aMaxFileSize) {
    this.maxFileSize = aMaxFileSize;
  }
}
