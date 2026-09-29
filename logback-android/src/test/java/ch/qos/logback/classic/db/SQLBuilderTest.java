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
package ch.qos.logback.classic.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ch.qos.logback.classic.db.names.SimpleDBNameResolver;

public class SQLBuilderTest {

  private final SimpleDBNameResolver resolver = new SimpleDBNameResolver();

  @Test
  public void insertPropertiesSqlUsesResolvedNames() {
    resolver.setTableNamePrefix("t_");
    resolver.setColumnNameSuffix("_c");
    assertEquals("INSERT INTO t_logging_event_property (event_id_c, mapped_key_c, mapped_value_c) VALUES (?, ?, ?)",
        SQLBuilder.buildInsertPropertiesSQL(resolver));
  }

  @Test
  public void deleteExpiredLogsSqlComparesTimestampWithExpiry() {
    assertEquals("DELETE FROM logging_event WHERE timestmp <= 1234;",
        SQLBuilder.buildDeleteExpiredLogsSQL(resolver, 1234L));
  }

  @Test
  public void publicConstructorIsAvailable() {
    // the class exposes an implicit public constructor (kept for API compatibility)
    assertNotNull(new SQLBuilder());
  }
}
