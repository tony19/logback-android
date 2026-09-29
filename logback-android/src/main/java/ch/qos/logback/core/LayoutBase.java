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

import ch.qos.logback.core.spi.ContextAwareBase;

abstract public class LayoutBase<E> extends ContextAwareBase implements Layout<E>  {

  protected boolean started;
  
  String fileHeader;
  String fileFooter;
  String presentationHeader;
  String presentationFooter;
  
  @Override
  public void setContext(Context context) {
    this.context = context;
  }

  @Override
  public Context getContext() {
    return this.context;
  }

  @Override
  public void start() {
    started = true;
  }

  @Override
  public void stop() {
    started = false;
  }
  
  @Override
  public boolean isStarted() {
    return started;
  }
  
  @Override
  public String getFileHeader() {
    return fileHeader;
  }
  
  @Override
  public String getPresentationHeader() {
    return presentationHeader;
  }
  
  @Override
  public String getPresentationFooter() {
    return presentationFooter;
  }
  
  @Override
  public String getFileFooter() {
    return fileFooter;
  }

  @Override
  public String getContentType() {
    return "text/plain";
  }
  
  public void setFileHeader(String header) {
    this.fileHeader = header;
  }

  public void setFileFooter(String footer) {
    this.fileFooter = footer;
  }
  
  public void setPresentationHeader(String header) {
    this.presentationHeader = header;
  }

  public void setPresentationFooter(String footer) {
    this.presentationFooter = footer;
  }
}
