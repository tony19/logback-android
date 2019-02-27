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
package ch.qos.logback.core.joran.spi;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;


public class CaseCombinatorTest {

  CaseCombinator p = new CaseCombinator();
  
  
  @Test
  public void smoke() {
    CaseCombinator p = new CaseCombinator();
     
    List<String> result = p.combinations("a-B=");
    
    List<String> witness = new ArrayList<String>();
    witness.add("a-b=");
    witness.add("A-b=");
    witness.add("a-B=");
    witness.add("A-B=");
    assertEquals(witness, result);
  }
  
  @Test
  public void other() {
    List<String> result = p.combinations("aBCd");
    assertEquals(16, result.size());
    Set<String> witness = new HashSet<String>(result);
    // check that there are no duplicates
    assertEquals(16, witness.size());
  }
}
