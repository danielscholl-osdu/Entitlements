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

package org.opengroup.osdu.entitlements.v2.jdbc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCheck {

  private final JdbcTemplate jdbcTemplate;
  private final EntitlementsConfigurationProperties entitlementsProperties;

  @EventListener
  public void onApplicationEvent(ApplicationReadyEvent event) {
    checkDatabaseConfiguration();
  }

  public void checkDatabaseConfiguration() {
    String request = "SELECT schema_name\n" + "FROM information_schema.schemata;";
    List<String> data = jdbcTemplate.queryForList(request, String.class);

    if (Objects.nonNull(data) && data.contains(entitlementsProperties.getDatastoreSchema())) {
      log.debug("Schema {} exists in DB.", entitlementsProperties.getDatastoreSchema());
    } else {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
          "Schema " + entitlementsProperties.getDatastoreSchema() + " does not exist in DB.");
    }
  }
}
