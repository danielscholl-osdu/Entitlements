/*
 *  Copyright 2020-2023 Google LLC
 *  Copyright 2020-2023 EPAM Systems, Inc
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

package org.opengroup.osdu.entitlements.v2.jdbc.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.config.DatabaseCheck;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@EnableConfigurationProperties(value = EntitlementsConfigurationProperties.class)
@RunWith(SpringRunner.class)
public class DatabaseCheckTest {
  @Mock private JdbcTemplate jdbcTemplate;

  @Autowired private EntitlementsConfigurationProperties entitlementsProperties;

  @Test
  public void check_databaseCheck_with_correct_schema() {
    DatabaseCheck databaseCheck = new DatabaseCheck(jdbcTemplate, entitlementsProperties);
    List<String> schemaList = Arrays.asList(new String[] {"entitlements_2", "entitlements_1"});
    when(jdbcTemplate.queryForList(
            "SELECT schema_name\n" + "FROM information_schema.schemata;", String.class))
        .thenReturn(schemaList);

    databaseCheck.checkDatabaseConfiguration();
  }

  @Test
  public void check_databaseCheck_with_notCorrect_schema() {
    DatabaseCheck databaseCheck = new DatabaseCheck(jdbcTemplate, entitlementsProperties);
    List<String> schemaList = Arrays.asList(new String[] {"entitlements_2", "entitlements_3"});
    when(jdbcTemplate.queryForList(
            "SELECT schema_name\n" + "FROM information_schema.schemata;", String.class))
        .thenReturn(schemaList);

    AppException appException =
        assertThrows(AppException.class, () -> databaseCheck.checkDatabaseConfiguration());

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), appException.getError().getCode());
  }

  @Test
  public void check_databaseCheck_with_nullSchemaList() {
    DatabaseCheck databaseCheck = new DatabaseCheck(jdbcTemplate, entitlementsProperties);
    when(jdbcTemplate.queryForList(
            "SELECT schema_name\n" + "FROM information_schema.schemata;", String.class))
        .thenReturn(null);

    AppException appException =
        assertThrows(AppException.class, () -> databaseCheck.checkDatabaseConfiguration());

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), appException.getError().getCode());
  }
}
