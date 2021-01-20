package org.opengroup.osdu.entitlements.v2.gcp.spi.redis.updateappids;

import io.github.resilience4j.retry.Retry;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.BaseRepo;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector;
import org.opengroup.osdu.entitlements.v2.gcp.spi.redis.operation.NodeMetaDataUpdateOperationImpl;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class UpdateAppIdsRepoRedis extends BaseRepo implements UpdateAppIdsRepo {

    @Autowired
    private RedisConnector redisConnector;

    @Autowired
    private JaxRsDpsLog log;

    @Autowired
    private GcpAppProperties config;

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private Retry retry;

    @Override
    public void run(EntityNode groupNode, Set<String> allowedAppIds) {
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
