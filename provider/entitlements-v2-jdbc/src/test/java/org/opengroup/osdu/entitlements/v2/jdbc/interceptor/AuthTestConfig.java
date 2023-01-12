/*
 *  Copyright 2020-2022 Google LLC
 *  Copyright 2020-2022 EPAM Systems, Inc
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

package org.opengroup.osdu.entitlements.v2.jdbc.interceptor;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.jdbc.config.EntOpenIDProviderConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.config.IDTokenValidatorFactory;
import org.opengroup.osdu.entitlements.v2.jdbc.config.security.ExternalAuthConfiguration;
import org.opengroup.osdu.entitlements.v2.jdbc.config.security.InternalAuthConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

@EnableConfigurationProperties
@ComponentScan(
    value = {
      "org.opengroup.osdu.entitlements.v2.jdbc.config",
      "org.opengroup.osdu.entitlements.v2.jdbc.interceptor"
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          value = {
            IDTokenValidatorFactory.class,
            ExternalAuthConfiguration.class,
            InternalAuthConfiguration.class,
            EntOpenIDProviderConfig.class
          })
    })
@Configuration
public class AuthTestConfig {
  public static final String TOKEN_WITH_NOT_CORRECT_SECRET =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0SXNzdWVySWQiLCJpYXQiOjE2Mzg5NzExNDksImV4cCI6MTY3MDUwNzE0OSwiYXVkIjoidGVzdENsaWVudElkIiwic3ViIjoidGVzdFVzZXJOYW1lQGV4YW1wbGUuY29tIn0.pvVPfim8r5TNyWX8gyzu46CvqLNsuuPP5Ltkt_RgNRc";
  public static final String IAP_EMAIL_PREFIX = "accounts.google.com:";
  public static final String MATCHING_USER_EMAIL = "testUserName@example.com";
  public static final String NOT_MATCHING_USER_EMAIL = "testUserName2@example.com";

  @Bean
  @Primary
  public DpsHeaders getDpsHeaders() {
    return new DpsHeaders();
  }

  @Bean
  @Primary
  public EntOpenIDProviderConfig getEntOpenIDProviderConfig() {
    return new EntOpenIDProviderConfig();
  }
}
