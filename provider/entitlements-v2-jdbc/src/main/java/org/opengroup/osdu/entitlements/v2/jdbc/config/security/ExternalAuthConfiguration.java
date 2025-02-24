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

package org.opengroup.osdu.entitlements.v2.jdbc.config.security;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.Application;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationFilter;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.jdbc.security.ExternalAuthorizationFilter;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@ConditionalOnExpression(
    value = "'${authentication-mode}'=='IAP' || '${authentication-mode}'=='EXTERNAL'")
@ComponentScan(
    value = {
      "org.opengroup.osdu.core",
      "org.opengroup.osdu.jdbc",
      "org.opengroup.osdu.entitlements.v2"
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          value = {AuthorizationFilter.class, Application.class})
    })
@Configuration
public class ExternalAuthConfiguration {

  @Bean
  public ExternalAuthorizationFilter authorizationFilter(
      AuthorizationService authService,
      RequestInfo requestInfo,
      JaxRsDpsLog log,
      RequestInfoUtilService requestInfoUtilService) {
    return new ExternalAuthorizationFilter(authService, requestInfo, log, requestInfoUtilService);
  }
}
