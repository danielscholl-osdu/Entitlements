/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.util;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.info.ConnectedOuterServicesBuilder;
import org.opengroup.osdu.core.common.model.info.ConnectedOuterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@ConditionalOnMissingBean(type = "ConnectedOuterServicesBuilder")
@Slf4j
@RequestScope
public class CloudConnectedOuterServicesBuilder implements ConnectedOuterServicesBuilder {

  private static final String REDIS_PREFIX = "Redis-";

  private final List<RedisCache> redisCaches;

  public CloudConnectedOuterServicesBuilder(List<RedisCache> redisCaches) {
    this.redisCaches = redisCaches;
  }

  @Override
  public List<ConnectedOuterService> buildConnectedOuterServices() {
    return redisCaches.stream()
        .map(this::fetchRedisInfo)
        .collect(Collectors.toList());
  }

  private ConnectedOuterService fetchRedisInfo(RedisCache cache) {
    String redisVersion = StringUtils.substringBetween(cache.info(), ":", "\r");
    return ConnectedOuterService.builder()
        .name(REDIS_PREFIX + StringUtils.substringAfterLast(cache.getClass().getName(), "."))
        .version(redisVersion)
        .build();
  }}