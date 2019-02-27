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
 * Adds custom prefix/suffix to table and column names.
 *
 * @author Tomasz Nurkiewicz
 * @since 0.9.20
 */
public class SimpleDBNameResolver implements DBNameResolver {

  private String tableNamePrefix = "";

  private String tableNameSuffix = "";

  private String columnNamePrefix = "";

  private String columnNameSuffix = "";

  public <N extends Enum<?>> String getTableName(N tableName) {
    return tableNamePrefix + tableName.name().toLowerCase(Locale.US) + tableNameSuffix;
  }

  public <N extends Enum<?>> String getColumnName(N columnName) {
    return columnNamePrefix + columnName.name().toLowerCase(Locale.US) + columnNameSuffix;
  }

  public void setTableNamePrefix(String tableNamePrefix) {
    this.tableNamePrefix = tableNamePrefix != null? tableNamePrefix : "";
  }

  public void setTableNameSuffix(String tableNameSuffix) {
    this.tableNameSuffix = tableNameSuffix != null? tableNameSuffix : "";
  }

  public void setColumnNamePrefix(String columnNamePrefix) {
    this.columnNamePrefix = columnNamePrefix != null? columnNamePrefix : "";
  }

  public void setColumnNameSuffix(String columnNameSuffix) {
    this.columnNameSuffix = columnNameSuffix != null? columnNameSuffix : "";
  }
}
