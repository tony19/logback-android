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
package ch.qos.logback.classic.turbo;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;

/**
 * This class allows output of debug level events to a certain list of users.
 *
 * If the level passed as a parameter is of level DEBUG, then the "user" value
 * taken from the MDC is checked against the configured user list. When the user
 * belongs to the list, the request is accepted. Otherwise a NEUTRAL response
 * is sent, thus not influencing the filter chain.
 *
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 */
public class DebugUsersTurboFilter extends TurboFilter {
  private static final String USER_MDC_KEY = "user";
  List<String> userList = new ArrayList<String>();

  @Override
  public FilterReply decide(List<Marker> markers, Logger logger, Level level, String format, Object[] params, Throwable t) {
    if (!level.equals(Level.DEBUG)) {
      return FilterReply.NEUTRAL;
    }
    String user = MDC.get(USER_MDC_KEY);
    if (user != null && userList.contains(user)) {
      return FilterReply.ACCEPT;
    }
    return FilterReply.NEUTRAL;
  }

  public void addUser(String user) {
    userList.add(user);
  }

  //test in BasicJoranTest only, to be removed asap.
  public List<String> getUsers() {
    return userList;
  }

}
