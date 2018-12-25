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
