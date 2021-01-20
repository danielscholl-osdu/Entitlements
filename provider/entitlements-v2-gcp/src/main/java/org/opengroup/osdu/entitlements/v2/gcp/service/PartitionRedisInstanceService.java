package org.opengroup.osdu.entitlements.v2.gcp.service;

public interface PartitionRedisInstanceService {

    /**
     * get host of redis instance from cache
     * if not found in cache get it from ccm
     * if not found in ccm return optional empty
     */
    String getHostOfRedisInstanceForPartition(String dataPartitionId);
}
