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
package ch.qos.logback.core.testUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.junit.MatcherAssume.assumeThat;

public class NetworkTestUtil {
  public void assumeNoUnresolvedUrlFallback() {
    // FIXME: This test fails when the ISP does not return 404 for
    // unknown URLs (required to cause an exception upon opening
    // the URL request). This was observed when running tests
    // while tethered to bluetooth mobile hotspot. The fix would
    // be to refactor AbstractIncludeAction to inject a URL opener.
    InetAddress addr;
    try {
      addr = InetAddress.getByName("not.a.valid.domain.name");
    } catch (UnknownHostException ex) {
      addr = null;
    }
    assumeThat(addr, is(nullValue()));
  }
}
