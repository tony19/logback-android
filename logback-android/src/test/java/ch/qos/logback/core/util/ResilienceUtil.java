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

import static junit.framework.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResilienceUtil {

  
  static public void verify(String logfile, String regexp, long totalSteps, double successRatioLowerBound) throws NumberFormatException, IOException {
    FileReader fr = new FileReader(logfile);
    BufferedReader br = new BufferedReader(fr);
    Pattern p = Pattern.compile(regexp);
    String line;
    
    int totalLines = 0;
    int oldNum = -1;
    int gaps = 0;
    while ((line = br.readLine()) != null) {
      Matcher m = p.matcher(line);
      if (m.matches()) {
        totalLines++;
        String g = m.group(1);
        int num = Integer.parseInt(g);
        if(oldNum != -1 && num != oldNum+1) {
          gaps++;
        }
        oldNum = num;
      }
    }
    fr.close();
    br.close();

    int lowerLimit = (int) (totalSteps*successRatioLowerBound);
    assertTrue("totalLines="+totalLines+" less than "+lowerLimit, totalLines > lowerLimit);
    
    // we want at least one gap indicating recuperation
    assertTrue("gaps="+gaps+" less than 1", gaps >= 1);
    
  }
}
