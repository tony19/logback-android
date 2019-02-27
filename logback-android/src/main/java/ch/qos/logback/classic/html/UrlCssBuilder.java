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
package ch.qos.logback.classic.html;

import ch.qos.logback.core.html.CssBuilder;


/**
 * This class helps the HTMLLayout build the CSS link.
 * It either provides the HTMLLayout with a default css file,
 * or builds the link to an external, user-specified, file.
 *
 * @author S&eacute;bastien Pennec
 */
public class UrlCssBuilder implements CssBuilder {

  String url = "http://logback.qos.ch/css/classic.css";
  
  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public void addCss(StringBuilder sbuf) {
    sbuf.append("<link REL=StyleSheet HREF=\"");
    sbuf.append(url);
    sbuf.append("\" TITLE=\"Basic\" />");
  }
}
