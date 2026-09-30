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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SimpleDBNameResolverTest {

  private final SimpleDBNameResolver resolver = new SimpleDBNameResolver();

  @Test
  public void namesAreLowerCaseEnumNamesByDefault() {
    assertEquals("logging_event_exception", resolver.getTableName(TableName.LOGGING_EVENT_EXCEPTION));
    assertEquals("caller_filename", resolver.getColumnName(ColumnName.CALLER_FILENAME));
  }

  @Test
  public void tableNamesGetConfiguredPrefixAndSuffix() {
    resolver.setTableNamePrefix("pre_");
    resolver.setTableNameSuffix("_suf");
    assertEquals("pre_logging_event_suf", resolver.getTableName(TableName.LOGGING_EVENT));
    // table affixes do not apply to columns
    assertEquals("event_id", resolver.getColumnName(ColumnName.EVENT_ID));
  }

  @Test
  public void columnNamesGetConfiguredPrefixAndSuffix() {
    resolver.setColumnNamePrefix("c_");
    resolver.setColumnNameSuffix("_x");
    assertEquals("c_event_id_x", resolver.getColumnName(ColumnName.EVENT_ID));
    // column affixes do not apply to tables
    assertEquals("logging_event", resolver.getTableName(TableName.LOGGING_EVENT));
  }

  @Test
  public void nullAffixesAreTreatedAsEmpty() {
    resolver.setTableNamePrefix("pre_");
    resolver.setTableNameSuffix("_suf");
    resolver.setColumnNamePrefix("c_");
    resolver.setColumnNameSuffix("_x");

    resolver.setTableNamePrefix(null);
    resolver.setTableNameSuffix(null);
    resolver.setColumnNamePrefix(null);
    resolver.setColumnNameSuffix(null);

    assertEquals("logging_event", resolver.getTableName(TableName.LOGGING_EVENT));
    assertEquals("event_id", resolver.getColumnName(ColumnName.EVENT_ID));
  }
}
