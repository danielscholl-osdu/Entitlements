// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.opengroup.osdu.entitlements.v2.aws.spi.updateappids;


import io.github.resilience4j.retry.Retry;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.spi.BaseRepo;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.aws.spi.operation.NodeMetaDataUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class AwsUpdateAppIdsRepo extends BaseRepo implements UpdateAppIdsRepo {

    @Autowired
    private RedisConnector redisConnector;

    @Autowired
    private JaxRsDpsLog log;

    @Autowired
    private AwsAppProperties config;

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private Retry retry;

    @Override
    public void updateAppIds(EntityNode groupNode, Set<String> allowedAppIds) {
        log.info(String.format("Updating allowed appids for group %s to %s in redis", groupNode, allowedAppIds));
        try {
            executeAppIdUpdate(groupNode, allowedAppIds);
        } catch (Exception ex) {
            rollback(ex);
            auditLogger.updateAppIds(AuditStatus.FAILURE, groupNode.getNodeId(), allowedAppIds);
            throw ex;
        } finally {
            executedCommands.clear();
        }
        auditLogger.updateAppIds(AuditStatus.SUCCESS, groupNode.getNodeId(), allowedAppIds);
    }

    private void executeAppIdUpdate(EntityNode groupEntityNode, Set<String> appIds) {
        Operation nodeMetaDataUpdateOperation = NodeMetaDataUpdateOperationImpl.builder().redisConnector(redisConnector)
                .retry(retry).log(log).config(config).groupNode(groupEntityNode).appIds(appIds).build();
        nodeMetaDataUpdateOperation.execute();
        executedCommands.push(nodeMetaDataUpdateOperation);
    }
}
