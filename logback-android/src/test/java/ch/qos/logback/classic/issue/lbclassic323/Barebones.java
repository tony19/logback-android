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
package ch.qos.logback.classic.issue.lbclassic323;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;

public class Barebones {

  public static void main(String[] args) {
    Context context = new ContextBase();
    for(int i = 0; i < 3; i++) {
      SenderRunnable senderRunnable = new SenderRunnable(""+i);
      context.getScheduledExecutorService().execute(senderRunnable);
    }
    System.out.println("done");
    //System.exit(0);
  }

  static class SenderRunnable implements Runnable {
    String id;
    SenderRunnable(String id) {
      this.id = id;
    }

    public void run() {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e) {
      }
      System.out.println("SenderRunnable " +id);
    }
  }
}
