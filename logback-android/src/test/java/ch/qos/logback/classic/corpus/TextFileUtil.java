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
package ch.qos.logback.classic.corpus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TextFileUtil {

  
  public static List<String> toWords(URL url) throws IOException {
    InputStream is = url.openStream();
    InputStreamReader reader = new InputStreamReader(is);
    BufferedReader br = new BufferedReader(reader);
    return toWords(br);
  }

  public static List<String> toWords(String filename) throws IOException {
    FileReader fr = new FileReader(filename);
    BufferedReader br = new BufferedReader(fr);
    return toWords(br);
  }
  
  public static List<String> toWords(BufferedReader br) throws IOException {

    // (\\d+)$
    //String regExp = "^(\\d+) "+ msg +  " ([\\dabcdef-]+)$";
    //Pattern p = Pattern.compile(regExp);
    String line;
    
   List<String> wordList = new ArrayList<String>();
    
    while ((line = br.readLine()) != null) {
      //line = line.replaceAll("\\p{Punct}+", " ");
      String[] words = line.split("\\s");
      for(String word: words) {
        wordList.add(word);
      }
    }  
    br.close();
  
    return wordList;
  }
}
