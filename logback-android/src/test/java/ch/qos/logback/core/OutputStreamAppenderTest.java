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
package ch.qos.logback.core;


import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.pattern.parser.SamplePatternLayout;

public class OutputStreamAppenderTest {

  Context context = new ContextBase();
  
  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void smoke() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER = "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }

  @Test
  public void nullFileHeader() {
    String FILE_HEADER = null;
    String PRESENTATION_HEADER = "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }

  @Test
  public void nullPresentationHeader() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER = null;
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }

  @Test
  public void nullPresentationFooter() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER =  "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = null;
    String FILE_FOOTER = "FILE_FOOTER";
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }
  
  @Test
  public void nullFileFooter() {
    String FILE_HEADER = "FILE_HEADER ";
    String PRESENTATION_HEADER = "PRESENTATION_HEADER";
    String PRESENTATION_FOOTER = "PRESENTATION_FOOTER ";
    String FILE_FOOTER = null;
    headerFooterCheck(FILE_HEADER, PRESENTATION_HEADER, PRESENTATION_FOOTER, FILE_FOOTER);
  }
  
  public void headerFooterCheck(String fileHeader, String presentationHeader, String presentationFooter, String fileFooter) {
    OutputStreamAppender<Object> wa = new OutputStreamAppender<Object>();
    wa.setContext(context);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
 
    SamplePatternLayout<Object> spl = new SamplePatternLayout<Object>();
    spl.setContext(context);
  
    spl.setFileHeader(fileHeader);
    spl.setPresentationHeader(presentationHeader);
    spl.setPresentationFooter(presentationFooter);
    spl.setFileFooter(fileFooter);
  
    spl.start();
    LayoutWrappingEncoder<Object> encoder = new LayoutWrappingEncoder<Object>();
    encoder.setLayout(spl);
    encoder.setContext(context);
    
    wa.setEncoder(encoder);
    wa.setOutputStream(baos);
    wa.start();
    
    wa.stop();
    String result = baos.toString();

    String expectedHeader = emtptyIfNull(fileHeader) + emtptyIfNull(presentationHeader);

    System.out.println(result);
    assertTrue(result, result.startsWith(expectedHeader));

    String expectedFooter = emtptyIfNull(presentationFooter) + emtptyIfNull(fileFooter);
    assertTrue(result, result.endsWith(expectedFooter));
  }
  
  String emtptyIfNull(String s) {
    return s == null ? "" : s;
  }
}
