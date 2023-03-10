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

package org.opengroup.osdu.entitlements.v2.jdbc.service;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = TARGET_CLASS)
@RequiredArgsConstructor
public class PartitionInfoFactory implements FactoryBean<PartitionInfo> {

  private final ICache<String, PartitionInfo> partitionInfoCache;

  private final IPartitionProvider partitionProvider;

  private final DpsHeaders dpsHeaders;

  @Override
  public PartitionInfo getObject() {
    String partitionId = dpsHeaders.getPartitionId();
    try {
      PartitionInfo partitionInfo = partitionInfoCache.get(partitionId);
      if (partitionInfo == null) {
        partitionInfo = partitionProvider.get(partitionId);
        partitionInfoCache.put(partitionId, partitionInfo);
      }
      return partitionInfo;
    } catch (PartitionException e) {
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "Partition service error.", "Unable to get Partition info for: " + partitionId, e);
    }
  }

  @Override
  public Class<?> getObjectType() {
    return PartitionInfo.class;
  }
}
