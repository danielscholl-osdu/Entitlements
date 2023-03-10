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

package org.opengroup.osdu.entitlements.v2.jdbc.util;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartitionIndexerServiceAccUtil {

  public static final String TENANT_INDEXER_SERVICE_ACCOUNT = "indexer.service.account";
  private final PartitionInfo partitionInfo;

  public String getTenantIndexerServiceAccount() {
    Property indexerServiceAcc = partitionInfo.getProperties().get(TENANT_INDEXER_SERVICE_ACCOUNT);
    if (indexerServiceAcc != null) {
      return indexerServiceAcc.getValue().toString();
    } else {
      return null;
    }
  }

  public String getTenantIndexerServiceAccCacheKey(String indexerServiceAcc, String dataPartitionId) {
    EntityNode indexerNode = EntityNode.createMemberNodeForNewUser(indexerServiceAcc,
        dataPartitionId);
    return indexerNode.getUniqueIdentifier() + "-data-groups";
  }
}
