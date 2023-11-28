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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.DEFAULT_SA;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.DEFAULT_SCHEMA;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.NOT_VALID_TENANT;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.TENANT_2_SA;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.TENANT_2_SCHEMA;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.TENANT_3_SA;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.TENANT_3_SCHEMA;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.TEST_SYSTEM_TENANT_1;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.TEST_TENANT_2;
import static org.opengroup.osdu.entitlements.v2.jdbc.config.MultiTenantJdbcTestConfig.TEST_TENANT_3;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.IPropertyResolver;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntConfigProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.interceptor.authenticator.IAuthenticator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MultiTenantJdbcTestConfig.class)
public class MultiTenantJdbcTemplateTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private MultiTenantRoutingDatasource multiTenantRoutingDatasource;

  @Autowired
  private EntConfigProperties properties;

  @Autowired
  private IPropertyResolver propertyResolver;

  @MockBean
  private IPartitionProvider partitionProvider;

  @MockBean
  private IAuthenticator authenticator;

  @Before
  public void setUp() throws SQLException {
    try (var conn = jdbcTemplate.getDataSource().getConnection()) {
      Statement statement = conn.createStatement();
      statement.execute(
          String.format(
              """ 
                      CREATE SCHEMA IF NOT EXISTS %s;
                      CREATE SCHEMA IF NOT EXISTS %s;
                      CREATE USER IF NOT EXISTS %s PASSWORD '%s' ADMIN;
                      CREATE USER IF NOT EXISTS %s PASSWORD '%s' ADMIN;
                  """
              ,
              TENANT_2_SCHEMA,
              TENANT_3_SCHEMA,
              TENANT_2_SA, TENANT_2_SA,
              TENANT_3_SA, TENANT_3_SA
          )
      );
    }
  }

  @Test
  public void testDefaultDatasource() throws SQLException {
    DataSource dataSource = jdbcTemplate.getDataSource();
    try (var connection = dataSource.getConnection()) {
      assertFalse(connection.isClosed());
      assertTrue(connection.isValid(10));
    }

    Map<Object, DataSource> dataSources = this.multiTenantRoutingDatasource.getResolvedDataSources();
    DataSource tenant1DataSource = dataSources.get(TEST_SYSTEM_TENANT_1);

    try (var conn = tenant1DataSource.getConnection()) {
      assertEquals(DEFAULT_SCHEMA, conn.getSchema());
      assertEquals(DEFAULT_SA, conn.getMetaData().getUserName());
    }
  }

  @Test
  public void testSingleTenantMultiThread() throws InterruptedException, SQLException {
    ConcurrentHashMap<String, Integer> tenant1Collector = new ConcurrentHashMap<>();
    TenantConnectionTester tenant1Tester = new TenantConnectionTester(
        TEST_SYSTEM_TENANT_1,
        tenant1Collector
    );
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 100; i++) {
      executorService.execute(tenant1Tester);
    }
    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);

    assertEquals(Integer.valueOf(100), tenant1Collector.get(DEFAULT_SA));
    assertEquals(1, tenant1Collector.size());

    Map<Object, DataSource> dataSources = this.multiTenantRoutingDatasource.getResolvedDataSources();
    DataSource tenant1DataSource = dataSources.get(TEST_SYSTEM_TENANT_1);

    try (var conn = tenant1DataSource.getConnection()) {
      assertEquals(DEFAULT_SCHEMA, conn.getSchema());
      assertEquals(DEFAULT_SA, conn.getMetaData().getUserName());
    }
  }

  @Test
  public void testMultiThreadingAndMultiTenant() throws InterruptedException {
    ConcurrentHashMap<String, Integer> tenant1Collector = new ConcurrentHashMap<>();
    TenantConnectionTester tenant1Tester = new TenantConnectionTester(TEST_SYSTEM_TENANT_1,
        tenant1Collector);

    ConcurrentHashMap<String, Integer> tenant2Collector = new ConcurrentHashMap<>();
    TenantConnectionTester tenant2tester = new TenantConnectionTester(TEST_TENANT_2,
        tenant2Collector);

    ConcurrentHashMap<String, Integer> tenant3Collector = new ConcurrentHashMap<>();
    TenantConnectionTester tenant3tester = new TenantConnectionTester(TEST_TENANT_3,
        tenant3Collector);

    ExecutorService executorService = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 100; i++) {
      executorService.execute(tenant1Tester);
      executorService.execute(tenant2tester);
      executorService.execute(tenant3tester);
    }

    executorService.shutdown();
    executorService.awaitTermination(1, TimeUnit.MINUTES);

    assertEquals(Integer.valueOf(100), tenant1Collector.get(DEFAULT_SA));
    assertEquals(1, tenant1Collector.size());

    assertEquals(Integer.valueOf(100), tenant2Collector.get(TENANT_2_SA));
    assertEquals(1, tenant2Collector.size());

    assertEquals(Integer.valueOf(100), tenant3Collector.get(TENANT_3_SA));
    assertEquals(1, tenant3Collector.size());

    assertEquals(3, multiTenantRoutingDatasource.getResolvedDataSources().size());
  }

  @Test
  public void testNotValidConfigShouldNotBeInMap() throws InterruptedException {
    TenantConnectionTester notValidTester = new TenantConnectionTester(
        NOT_VALID_TENANT,
        new ConcurrentHashMap<>()
    );
    var executorService = Executors.newFixedThreadPool(1);
    executorService.execute(notValidTester);
    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.SECONDS);
    Map<Object, DataSource> dataSources = this.multiTenantRoutingDatasource.getResolvedDataSources();
    assertNull(dataSources.get(NOT_VALID_TENANT));
  }

  class TenantConnectionTester implements Runnable {

    private final String tenant;
    private final ConcurrentMap<String, Integer> assertionCollector;

    public TenantConnectionTester(String tenant,
        ConcurrentMap<String, Integer> assertionCollector) {
      this.tenant = tenant;
      this.assertionCollector = assertionCollector;
    }

    @Override
    public void run() {
      ThreadLocalTenantStorage.setTenantName(tenant);
      DataSource dataSource = jdbcTemplate.getDataSource();
      try (var connection = dataSource.getConnection()) {
        DatabaseMetaData metaData = connection.getMetaData();
        String tenantUser = metaData.getUserName();
        assertionCollector.compute(tenantUser, (key, value) -> {
          if (value != null) {
            return value + 1;
          } else {
            return 1;
          }
        });
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
      ThreadLocalTenantStorage.clear();
    }
  }
}
