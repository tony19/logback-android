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

import java.io.ByteArrayOutputStream;

public class ByteArrayUtil {

  // big-endian
  static void writeInt(byte[] byteArray, int offset, int i) {
    for (int j = 0; j < 4; j++) {
      int shift = 24 - j * 8;
      byteArray[offset + j] = (byte) (i >>> shift);
    }
  }

  static void writeInt(ByteArrayOutputStream baos, int i) {
    for (int j = 0; j < 4; j++) {
      int shift = 24 - j * 8;
      baos.write((byte) (i >>> shift));
    }
  }
  
  // big-endian  
  static int readInt(byte[] byteArray, int offset) {
    int i = 0;
    for (int j = 0; j < 4; j++) {
      int shift = 24 - j * 8;
      i += (byteArray[offset + j] & 0xFF) << shift;
    }
    return i;
  }
  
  static public String toHexString(byte[] ba) {
    StringBuilder sbuf = new StringBuilder();
    for(byte b: ba) {
      String s = Integer.toHexString( (int)(b & 0xff));
      if(s.length() == 1) {
        sbuf.append('0');
      }
      sbuf.append(s);
    }
    return sbuf.toString();
  }

  static public byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] ba = new byte[len/2];

    for(int i = 0; i < ba.length; i++) {
      int j = i*2;
      int t = Integer.parseInt(s.substring(j, j+2), 16);
      byte b = (byte) (t & 0xFF);
      ba[i] = b;
    }
    return ba;
  }
}
