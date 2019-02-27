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
package ch.qos.logback.core.rolling.helper;


/**
 * <code>TokenConverter</code> offers some basic functionality used by more 
 * specific token  converters. 
 * <p>
 * It basically sets up the chained architecture for tokens. It also forces 
 * derived classes to fix their type.
 * 
 * @author Ceki
 * @since 1.3
 */
public class TokenConverter {
  
  
  static final int IDENTITY = 0;
  static final int INTEGER = 1;
  static final int DATE = 1;
  int type;
  TokenConverter next;

  protected TokenConverter(int t) {
    type = t;
  }

  public TokenConverter getNext() {
    return next;
  }

  public void setNext(TokenConverter next) {
    this.next = next;
  }
 
  public int getType() {
    return type;
  }

  public void setType(int i) {
    type = i;
  }

}
