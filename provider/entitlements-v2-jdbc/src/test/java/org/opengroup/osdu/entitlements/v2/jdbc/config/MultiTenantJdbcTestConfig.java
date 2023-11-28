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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opengroup.osdu.core.common.partition.IPropertyResolver;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class MultiTenantJdbcTestConfig {

  public static final String TEST_SYSTEM_TENANT_1 = "tenant-1";
  public static final String TEST_TENANT_2 = "tenant-2";
  public static final String TEST_TENANT_3 = "tenant-3";

  public static final String PROPERTY_URL = "test.datasource.url";
  public static final String PROPERTY_USER = "test.datasource.username";
  public static final String PROPERTY_PASS = "test.datasource.password";
  public static final String PROPERTY_SCHEMA = "test.datasource.schema";

  public static final String H2_CONNECTION = "jdbc:h2:mem:db;DB_CLOSE_DELAY=-1";
  public static final String DEFAULT_SA = "SA";
  public static final String TENANT_2_SA = "TENANT_2_SA";
  public static final String TENANT_3_SA = "TENANT_3_SA";
  public static final String NOT_VALID_TENANT = "NOT_VALID_TENANT";

  public static final String DEFAULT_SCHEMA = "PUBLIC";
  public static final String TENANT_2_SCHEMA = "TENANT_2";
  public static final String TENANT_3_SCHEMA = "TENANT_3";
  public static final String NOT_EXISTING_SCHEMA = "NOT_EXIST";

  @Bean
  @Primary
  public IPropertyResolver propertyResolver() {
    IPropertyResolver propertyResolver = mock(IPropertyResolver.class);

    when(propertyResolver.getPropertyValue(PROPERTY_URL, TEST_SYSTEM_TENANT_1)).thenReturn(H2_CONNECTION);
    when(propertyResolver.getPropertyValue(PROPERTY_USER, TEST_SYSTEM_TENANT_1)).thenReturn(DEFAULT_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_PASS, TEST_SYSTEM_TENANT_1)).thenReturn(DEFAULT_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_SCHEMA, TEST_SYSTEM_TENANT_1)).thenReturn(DEFAULT_SCHEMA);

    when(propertyResolver.getPropertyValue(PROPERTY_URL, TEST_TENANT_2)).thenReturn(H2_CONNECTION);
    when(propertyResolver.getPropertyValue(PROPERTY_USER, TEST_TENANT_2)).thenReturn(TENANT_2_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_PASS, TEST_TENANT_2)).thenReturn(TENANT_2_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_SCHEMA, TEST_TENANT_2)).thenReturn(TENANT_2_SCHEMA);

    when(propertyResolver.getPropertyValue(PROPERTY_URL, TEST_TENANT_3)).thenReturn(H2_CONNECTION);
    when(propertyResolver.getPropertyValue(PROPERTY_USER, TEST_TENANT_3)).thenReturn(TENANT_3_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_PASS, TEST_TENANT_3)).thenReturn(TENANT_3_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_SCHEMA, TEST_TENANT_3)).thenReturn(TENANT_3_SCHEMA);

    when(propertyResolver.getPropertyValue(PROPERTY_URL, NOT_VALID_TENANT)).thenReturn(H2_CONNECTION);
    when(propertyResolver.getPropertyValue(PROPERTY_USER, NOT_VALID_TENANT)).thenReturn(DEFAULT_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_PASS, NOT_VALID_TENANT)).thenReturn(DEFAULT_SA);
    when(propertyResolver.getPropertyValue(PROPERTY_SCHEMA, NOT_VALID_TENANT)).thenReturn(NOT_EXISTING_SCHEMA);

    return propertyResolver;
  }
}
