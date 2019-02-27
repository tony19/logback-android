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

import java.nio.charset.Charset;

import ch.qos.logback.core.CoreConstants;

public class DummyEncoder<E> extends EncoderBase<E> {

  public static final String DUMMY = "dummy" + CoreConstants.LINE_SEPARATOR;
  String val = DUMMY;
  String fileHeader;
  String fileFooter;
  Charset charset;

  public Charset getCharset() {
    return charset;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  public DummyEncoder() {
  }

  public DummyEncoder(String val) {
    this.val = val;
  }

  public byte[] encode(E event) {
    return encodeString(val);
  }

  byte[] encodeString(String s) {
    if (charset == null) {
      return s.getBytes();
    } else {
      return s.getBytes(charset);
    }
  }

  private void appendIfNotNull(StringBuilder sb, String s) {
    if (s != null) {
      sb.append(s);
    }
  }

  byte[] header() {
    StringBuilder sb = new StringBuilder();
    appendIfNotNull(sb, fileHeader);
    if (sb.length() > 0) {
      // If at least one of file header or presentation header were not
      // null, then append a line separator.
      // This should be useful in most cases and should not hurt.
      sb.append(CoreConstants.LINE_SEPARATOR);
    }
    return encodeString(sb.toString());
  }

  public byte[] headerBytes() {
    return header();
  }

  public byte[] footerBytes() {
    if (fileFooter == null) {
      return null;
    }
    return encodeString(fileFooter);
  }

  public String getFileHeader() {
    return fileHeader;
  }

  public void setFileHeader(String fileHeader) {
    this.fileHeader = fileHeader;
  }

  public String getFileFooter() {
    return fileFooter;
  }

  public void setFileFooter(String fileFooter) {
    this.fileFooter = fileFooter;
  }

}
