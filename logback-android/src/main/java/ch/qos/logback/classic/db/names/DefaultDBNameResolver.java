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
package ch.qos.logback.classic.db.names;

import java.util.Locale;

/**
 * The default name resolver simply returns the enum passes as parameter
 * as a lower case string.
 * 
 * @author Tomasz Nurkiewicz
 * @author Ceki Gulcu
 * @since  0.9.19
 */
public class DefaultDBNameResolver implements DBNameResolver {

  public <N extends Enum<?>> String getTableName(N tableName) {
    return tableName.toString().toLowerCase(Locale.US);
  }

  public <N extends Enum<?>> String getColumnName(N columnName) {
    return columnName.toString().toLowerCase(Locale.US);
  }

}
