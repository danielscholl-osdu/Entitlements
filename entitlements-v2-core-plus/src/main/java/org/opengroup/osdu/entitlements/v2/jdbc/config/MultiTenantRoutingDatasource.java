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

import static org.opengroup.osdu.entitlements.v2.jdbc.config.PropertyResolverUtil.getPartitionProperty;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.partition.IPropertyResolver;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntConfigProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class MultiTenantRoutingDatasource extends AbstractRoutingDataSource {

  public static final String DATASOURCE = ".datasource.";
  public static final String URL = DATASOURCE.concat("url");
  public static final String USERNAME = DATASOURCE.concat("username");
  public static final String PASSWORD = DATASOURCE.concat("password");
  public static final String SCHEMA = DATASOURCE.concat("schema");

  private final EntConfigProperties properties;
  private final IPropertyResolver propertyResolver;

  private ConcurrentMap<String, HikariDataSource> tenantDataSourceMap;

  @Override
  public void afterPropertiesSet() {
    tenantDataSourceMap = new ConcurrentHashMap<>();
  }

  @Override
  public DataSource getResolvedDefaultDataSource() {
    return getValidDataSource(properties.getSystemTenant());
  }

  @Override
  protected Object determineCurrentLookupKey() {
    return ThreadLocalTenantStorage.getTenantName();
  }

  @Override
  protected DataSource determineTargetDataSource() {
    String tenantId = (String) determineCurrentLookupKey();
    if (Objects.isNull(tenantId)) {
      return getResolvedDefaultDataSource();
    }
    return getValidDataSource(tenantId);
  }

  @Override
  public Map<Object, DataSource> getResolvedDataSources() {
    Assert.state(this.tenantDataSourceMap != null,
        "DataSources not resolved yet - call afterPropertiesSet");
    return tenantDataSourceMap.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
  }

  private HikariDataSource getValidDataSource(String tenantId) {
    return tenantDataSourceMap.compute(tenantId, (key, currentDataSource) -> {
      if (currentDataSource != null && !currentDataSource.isClosed()) {
        return currentDataSource;
      } else {
        if (currentDataSource != null) {
          currentDataSource.close();
        }
        return getDataSource(tenantId);
      }
    });
  }

  private HikariDataSource getDataSource(String partitionId) {
    String url = getPartitionProperty(properties, propertyResolver, URL, partitionId);
    String username = getPartitionProperty(properties, propertyResolver, USERNAME, partitionId);
    String password = getPartitionProperty(properties, propertyResolver, PASSWORD, partitionId);
    String schema = getPartitionProperty(properties, propertyResolver, SCHEMA, partitionId);

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(url);
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);
    hikariConfig.setSchema(schema);
    return new HikariDataSource(hikariConfig);
  }

  @PreDestroy
  private void shutDown() {
    tenantDataSourceMap.forEach((key, dataSource) -> {
          if (!dataSource.isClosed()) {
            dataSource.close();
          }
        }
    );
  }
}
