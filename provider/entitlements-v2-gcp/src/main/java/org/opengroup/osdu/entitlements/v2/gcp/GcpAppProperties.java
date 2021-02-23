package org.opengroup.osdu.entitlements.v2.gcp;

import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GcpAppProperties extends AppProperties {
    public static final String USERS = "service.entitlements.user";
    public static final String ADMIN = "service.entitlements.admin";
    public static final String OPS = "users.datalake.ops";
    public static final String DEFAULT_APPID_KEY = "no-app-id";
    public static final String REDIS_IP_OF_PARTITION_KEY_FORMAT = "%s-entitlements-db-redis-ip";
    @Value("${app.cronIp}")
    private String cronIp;
    @Value("${app.region}")
    private String region;
    @Value("${app.centralRedisInstId}")
    private String centralRedisInstId;
    @Value("${app.redisLocation}")
    private String redisInstLocation;
    @Value("${app.shared-vpc.host}")
    private String sharedVPCHostProjectId;
    @Value("${app.projectId}")
    private String projectId;
    @Value("${app.domain}")
    private String domain;
    @Value("${app.centralRedisInstIp}")
    private String centralRedisInstIp;
    @Value("${app.centralRedisInstPort}")
    private String centralRedisInstPort;
    @Value("${app.partition.redis.instance.id}")
    private String partitionRedisInstanceId;
    @Value("${app.redis.central.db.cached.items}")
    private int cachedItemsDb;
    @Value("${app.redis.central.db.partition.association}")
    private int partitionAssociationDb;
    @Value("${app.redis.central.db.list.group.cache}")
    private int listGroupCache;
    @Value("${app.partition.db.entitynode}")
    private int partitionEntityNodeDb;
    @Value("${app.partition.db.parent.ref}")
    private int partitionParentRefDb;
    @Value("${app.partition.db.children.ref}")
    private int partitionChildrenRefDb;
    @Value("${app.partition.db.appid}")
    private int partitionAppIdDb;
    @Value("${app.cache.update.queue.name}")
    private String cacheUpdateQueueName;
    @Value("${app.task.queue.location}")
    private String taskQueueLocation;

    public String getCRONIpAddress() {
        return cronIp;
    }

    public String getRegion() {
        return region;
    }

    public String getCentralRedisInstIp() {
        return centralRedisInstIp;
    }

    public int getCentralRedisInstPort() {
        return Integer.parseInt(centralRedisInstPort);
    }

    public void setCentralRedisInstPort(int port) {
        centralRedisInstPort = String.valueOf(port);
    }

    public String getRedisInstanceLocation() {
        return redisInstLocation;
    }

    public String getSharedVPCNetwork() {
        return String.format("projects/%s/global/networks/entitlements-v2-shared-vpc", sharedVPCHostProjectId);
    }

    public String getPartitionRedisInstanceId() {
        return partitionRedisInstanceId;
    }

    public String getCentralRedisInstanceId() {
        return centralRedisInstId;
    }

    public int getCachedItemsDb() {
        return cachedItemsDb;
    }

    public int getPartitionAssociationDb() {
        return partitionAssociationDb;
    }

    public int getlistGroupCacheDb() {
        return listGroupCache;
    }

    public int getPartitionEntityNodeDb() {
        return partitionEntityNodeDb;
    }

    public int getPartitionParentRefDb() {
        return partitionParentRefDb;
    }

    public int getPartitionChildrenRefDb() {
        return partitionChildrenRefDb;
    }

    public int getPartitionAppIdDb() {
        return partitionAppIdDb;
    }

    public String getCacheUpdateQueueName() {
        return cacheUpdateQueueName;
    }

    public String getTaskQueueLocation() {
        return taskQueueLocation;
    }

    @Override
    public List<String> getInitialGroups() {
        List<String> initialGroups = new ArrayList<>();
        initialGroups.add("/provisioning/groups/datalake_user_groups.json");
        initialGroups.add("/provisioning/groups/datalake_service_groups.json");
        initialGroups.add("/provisioning/groups/data_groups.json");
        return initialGroups;
    }

    @Override
    public String getGroupsOfServicePrincipal() {
        return "/provisioning/accounts/groups_of_service_principal.json";
    }
}
