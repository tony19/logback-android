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
package ch.qos.logback.core.encoder;

import static junit.framework.Assert.*;

import java.util.Random;

import org.junit.Test;

public class ByteArrayUtilTest {

  int BA_SIZE = 16;
  byte[] byteArray = new byte[BA_SIZE];

  Random random = new Random(18532235);
  
  @Test
  public void smoke() {
    verifyLoop(byteArray, 0, 0);
    verifyLoop(byteArray, 0, 10);
    verifyLoop(byteArray, 0, Integer.MAX_VALUE);
    verifyLoop(byteArray, 0, Integer.MIN_VALUE);
  }

  @Test
  public void random() {
    for(int i = 0; i < 100000; i++) {
      int rOffset = random.nextInt(BA_SIZE-4);
      int rInt = random.nextInt();
      verifyLoop(byteArray, rOffset, rInt);
    }
  }
  
  void verifyLoop(byte[] ba, int offset, int expected) {
    ByteArrayUtil.writeInt(byteArray, offset, expected);
    int back = ByteArrayUtil.readInt(byteArray, offset);
    assertEquals(expected, back);
    
  }

}
