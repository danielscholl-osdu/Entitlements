package org.opengroup.osdu.entitlements.v2.gcp.service;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.gcp.GcpAppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

// TODO: Remove when org.opengroup.osdu.entitlements.v2.gcp.spi.redis.connection.RedisConnector will be changed
@Service
public class PartitionRedisInstanceServiceImpl implements PartitionRedisInstanceService {

    @Autowired
    private JaxRsDpsLog log;

    @Autowired
    @Lazy
    private RequestInfo requestInfo;

    @Autowired
    private GcpAppProperties appProperties;

//    @Autowired
//    @Lazy
//    private CachedItemsRepo cachedItemsRepo;

    @Override
    public String getHostOfRedisInstanceForPartition(final String dataPartitionId) {
//        String projectId = requestInfo.getTenantInfo().getProjectId();
//        final String key = String.format(AppProperties.REDIS_IP_OF_PARTITION_KEY_FORMAT, projectId);
//        return cachedItemsRepo.getCachedItem(key).get();
        return null;
    }
}
