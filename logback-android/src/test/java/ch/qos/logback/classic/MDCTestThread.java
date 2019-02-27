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

import org.slf4j.MDC;

public class MDCTestThread extends Thread {
  
  String val;
  
  public MDCTestThread(String val) {
    super();
    this.val = val;
  }
  
  String x0;
  String x1;
  String x2;
  
  public void run() {
    x0 = MDC.get("x");
    MDC.put("x", val);
    x1 = MDC.get("x");
    MDC.clear();
    x2 = MDC.get("x");
    //System.out.println("Exiting "+val);
  }
} 