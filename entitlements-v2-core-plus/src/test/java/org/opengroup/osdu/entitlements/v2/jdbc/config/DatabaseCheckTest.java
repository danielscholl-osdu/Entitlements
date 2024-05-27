/*
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.partition.IPropertyResolver;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntConfigProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = {SpiJdbcTestConfig.class, MultiTenantJdbcTestConfig.class})
@ExtendWith(SpringExtension.class)
class DatabaseCheckTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private EntConfigProperties entitlementsProperties;

  @Autowired
  private IPropertyResolver propertyResolver;

  @Test
  void check_databaseCheck_with_correct_schema() {
    DatabaseCheck databaseCheck = new DatabaseCheck(jdbcTemplate, entitlementsProperties,
        propertyResolver);
    List<String> schemaList = Arrays.asList(new String[]{"PUBLIC"});
    when(jdbcTemplate.queryForList(
        "SELECT schema_name\n" + "FROM information_schema.schemata;", String.class))
        .thenReturn(schemaList);

    databaseCheck.checkDatabaseConfiguration();
  }

  @Test
  void check_databaseCheck_with_notCorrect_schema() {
    DatabaseCheck databaseCheck = new DatabaseCheck(jdbcTemplate, entitlementsProperties,
        propertyResolver);
    List<String> schemaList = Arrays.asList(new String[]{"entitlements_2", "entitlements_3"});
    when(jdbcTemplate.queryForList(
        "SELECT schema_name\n" + "FROM information_schema.schemata;", String.class))
        .thenReturn(schemaList);

    AppException appException =
        assertThrows(AppException.class, () -> databaseCheck.checkDatabaseConfiguration());

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), appException.getError().getCode());
  }

  @Test
  void check_databaseCheck_with_nullSchemaList() {
    DatabaseCheck databaseCheck = new DatabaseCheck(jdbcTemplate, entitlementsProperties,
        propertyResolver);
    when(jdbcTemplate.queryForList(
        "SELECT schema_name\n" + "FROM information_schema.schemata;", String.class))
        .thenReturn(null);

    AppException appException =
        assertThrows(AppException.class, () -> databaseCheck.checkDatabaseConfiguration());

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), appException.getError().getCode());
  }
}
