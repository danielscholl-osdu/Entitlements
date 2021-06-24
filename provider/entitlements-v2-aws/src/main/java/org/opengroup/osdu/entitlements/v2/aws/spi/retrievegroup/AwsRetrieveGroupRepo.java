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
package org.opengroup.osdu.entitlements.v2.aws.spi.retrievegroup;

import com.google.common.collect.Lists;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnectionPool;
import org.opengroup.osdu.entitlements.v2.aws.spi.db.RedisConnector;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.JsonConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties.DEFAULT_APPID_KEY;

@Repository
public class AwsRetrieveGroupRepo implements RetrieveGroupRepo {

    private static final int MAXIMUM_SCARD_SIZE = 100;
    private static final int SCAN_PAGE_SIZE = 200;

    @Autowired
    private RedisConnector redisConnector;

    @Autowired
    private AwsAppProperties config;

    @Autowired
    private JaxRsDpsLog log;

    @Override
    public EntityNode groupExistenceValidation(String groupId, String partitionId) {
        Optional<EntityNode> groupNode = getEntityNode(groupId, partitionId);
        return groupNode.orElseThrow(() -> {
            log.info(String.format("Can't find group %s", groupId));
            return new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), String.format("Group %s is not found", groupId));
        });
    }

    @Override
    public Optional<EntityNode> getEntityNode(String entityEmail, String partitionId) {
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(partitionId);
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionEntityNode());
            String groupNodeJson = commands.get(entityEmail);
            EntityNode groupNode = null;
            System.out.println("The value of GropNodeJson is " +groupNodeJson);
            if (!StringUtils.isEmpty(groupNodeJson)) {

                groupNode = JsonConverter.fromJson(groupNodeJson, EntityNode.class);
            }
            return Optional.ofNullable(groupNode);
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    @Override
    public EntityNode getMemberNodeForRemovalFromGroup(String memberId, String partitionId) {
        if (!memberId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))) {
            return EntityNode.createMemberNodeForNewUser(memberId, partitionId);
        }
        return EntityNode.createNodeFromGroupEmail(memberId);
    }

    @Override
    public Set<EntityNode> getEntityNodes(String partitionId, List<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptySet();
        }
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(partitionId);
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionEntityNode());
            log.info(String.format("Load nodes %s from partition %s", String.join(",", nodeIds), partitionId));
            List<KeyValue<String, String>> allParentsNodeJsons = commands.mget(nodeIds.toArray(new String[0]));
            return EntityNode.convertMemberNodeListFromListOfJson(
                    allParentsNodeJsons.stream().map(Value::getValue).collect(Collectors.toList()));
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    @Override
    public Map<String, Set<String>> getUserPartitionAssociations(final Set<String> userIds) {
        final RedisConnectionPool connectionPool = redisConnector.getRedisConnectionPool();
        final StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        final Map<String, Set<String>> dataPartitionsPerUserId = new HashMap<>();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getRedisPartitionAssociation());
            userIds.forEach(userId -> dataPartitionsPerUserId.put(userId, commands.smembers(userId)));
        } finally {
            connectionPool.returnConnection(connection, log);
        }
        return dataPartitionsPerUserId;
    }

    @Override
    public Map<String, Integer> getAllUserPartitionAssociations() {
        RedisConnectionPool connectionPool = this.redisConnector.getRedisConnectionPool();
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        log.info(String.format("Get redis connection correctly for central redis instance %s", config.getRedisHost()));
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getRedisPartitionAssociation());
            KeyScanCursor<String> cursor = commands.scan(ScanArgs.Builder.limit(SCAN_PAGE_SIZE));
            List<String> allKeys = cursor.getKeys();
            while (!cursor.isFinished()) {
                cursor = commands.scan(cursor);
                allKeys.addAll(cursor.getKeys());
            }
            return getAssociationCount(allKeys);
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    @Override
    public Map<String, Integer> getAssociationCount(List<String> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<List<String>> chunkedKeys = Lists.partition(userIds, MAXIMUM_SCARD_SIZE);
        Map<String, Integer> allUserAssociations = new HashMap<>();
        for (List<String> chunk : chunkedKeys) {
            ConcurrentMap<String, Integer> userAssociationMap = new ConcurrentHashMap<>();
            RedisConnectionPool connectionPool = this.redisConnector.getRedisConnectionPool();
            StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
            try {
                RedisAsyncCommands<String, String> asyncCommands = connection.async();
                asyncCommands.select(config.getRedisPartitionAssociation());
                RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
                chunk.parallelStream().forEach(userId -> {
                    RequestContextHolder.setRequestAttributes(attributes, true);
                    try {
                        userAssociationMap.put(userId,
                                asyncCommands.scard(userId).get().intValue());
                    } catch (Exception e) {
                        log.error(String.format("Error Retrieving partition association for user : %s", userId), e);
                    }
                });
                allUserAssociations.putAll(userAssociationMap);
            } finally {
                connectionPool.returnConnection(connection, log);
            }
        }
        return allUserAssociations;
    }

    @Override
    public Set<EntityNode> getAllGroupNodes(String partitionId, String partitionDomain) {
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(partitionId);
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionEntityNode());
            KeyScanCursor<String> cursor = commands.scan(ScanArgs.Builder.limit(SCAN_PAGE_SIZE).match(String.format("*@%s", partitionDomain)));
            List<String> allKeys = cursor.getKeys();
            while (!cursor.isFinished()) {
                cursor = commands.scan(cursor);
                allKeys.addAll(cursor.getKeys());
            }
            return getEntityNodes(partitionId, allKeys);
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    @Override
    public Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference) {
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(groupNode.getDataPartitionId());
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionChildrenRef());
            return commands.sismember(groupNode.getNodeId(), JsonConverter.toJson(childrenReference));
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    @Override
    public List<ParentReference> loadDirectParents(String partitionId, String... nodeId) {
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(partitionId);
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionParentRef());
            Set<String> parentRef = commands.sunion(nodeId);
            if (parentRef != null) {
                return parentRef.parallelStream().map(ref -> JsonConverter.fromJson(ref, ParentReference.class)).collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    @Override
    public ParentTreeDto loadAllParents(EntityNode memberNode) {
        Set<String> visited = new CopyOnWriteArraySet<>();
        List<ParentReference> directParents = loadDirectParents(memberNode.getDataPartitionId(), memberNode.getNodeId());

        Set<ParentReference> allParents = new HashSet<>(directParents);

        visited.add(memberNode.getNodeId());
        Deque<List<ParentReference>> queue = new LinkedList<>();
        if (!directParents.isEmpty()) {
            queue.addAll(Collections.singletonList(directParents));
        }
        int maxDepth = 1;
        while (!queue.isEmpty()) {
            ++maxDepth;
            List<ParentReference> groupRef = queue.stream().flatMap(List::stream).distinct().collect(Collectors.toList());
            queue.clear();
            Map<String, List<ParentReference>> nonVisitedRef = groupRef.parallelStream().filter(ref -> !visited.contains(ref.getId()))
                    .collect(Collectors.groupingBy(ParentReference::getDataPartitionId));
            nonVisitedRef.forEach((key, value) -> {
                List<String> allNodeIds = value.stream().map(ParentReference::getId).collect(Collectors.toList());
                List<ParentReference> intermediateParents = loadDirectParents(key, allNodeIds.toArray(new String[0]));
                allParents.addAll(intermediateParents);
                if (!intermediateParents.isEmpty()) {
                    queue.addAll(Collections.singletonList(intermediateParents));
                }
                visited.addAll(allNodeIds);
            });
        }
        Set<ParentReference> filteredAllParents = new HashSet<>();
        Iterator<ParentReference> it = allParents.iterator();
        while(it.hasNext()){
            ParentReference pf = it.next();
          if(pf.getDataPartitionId().equals(memberNode.getDataPartitionId()))
          {
              filteredAllParents.add(pf);
          }
        }
        return ParentTreeDto.builder().parentReferences(filteredAllParents).maxDepth(maxDepth).build();
    }

    @Override
    public List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeId) {
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(partitionId);
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionChildrenRef());
            Set<String> childRef = commands.sunion(nodeId);
            if (childRef != null) {
                return childRef.parallelStream().map(ref -> JsonConverter.fromJson(ref, ChildrenReference.class)).collect(Collectors.toList());
            } else {
                return new ArrayList<>();
            }
        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }

    @Override
    public Set<String> getGroupOwners(String partitionId, String nodeId) {
        return loadDirectChildren(partitionId, nodeId).stream().filter(ChildrenReference::isOwner).map(ChildrenReference::getId).collect(Collectors.toSet());
    }

    @Override
    public ChildrenTreeDto loadAllChildrenUsers(EntityNode node) {
        int maxDepth = 1;
        if (node.isUser()) {
            return ChildrenTreeDto.builder().maxDepth(maxDepth).childrenUserIds(Collections.singletonList(node.getNodeId())).build();
        }
        Set<String> visited = new CopyOnWriteArraySet<>();
        List<ChildrenReference> directChildren = loadDirectChildren(node.getDataPartitionId(), node.getNodeId());
        Set<ChildrenReference> allChildrenUser = directChildren.stream()
                .filter(ChildrenReference::isUser).collect(Collectors.toSet());
        visited.add(node.getNodeId());
        List<List<ChildrenReference>> queue = new LinkedList<>();
        if (!directChildren.isEmpty()) {
            queue.addAll(Collections.singletonList(directChildren));
        }
        while (!queue.isEmpty()) {
            ++maxDepth;
            List<ChildrenReference> childRef = queue.stream().flatMap(List::stream).distinct().collect(Collectors.toList());
            queue.clear();
            Map<String, List<ChildrenReference>> nonVisitedRef = childRef.parallelStream().filter(ref -> !visited.contains(ref.getId()))
                    .collect(Collectors.groupingBy(ChildrenReference::getDataPartitionId));
            nonVisitedRef.forEach((key, value) -> {
                List<String> allNodeIds = value.stream().map(ChildrenReference::getId).collect(Collectors.toList());
                List<ChildrenReference> intermediateChildren = loadDirectChildren(key, allNodeIds.toArray(new String[0]));
                allChildrenUser.addAll(intermediateChildren.stream().filter(ChildrenReference::isUser).collect(Collectors.toList()));
                List<ChildrenReference> filteredChildren = intermediateChildren.stream().filter(ChildrenReference::isGroup).collect(Collectors.toList());
                if (!filteredChildren.isEmpty()) {
                    queue.addAll(Collections.singletonList(filteredChildren));
                }
                visited.addAll(allNodeIds);
            });
        }
        return ChildrenTreeDto.builder().maxDepth(maxDepth)
                .childrenUserIds(allChildrenUser.stream().map(ChildrenReference::getId).distinct().collect(Collectors.toList())).build();
    }

    /**
     * This implementation use the fact that DEFAULT_APPID_KEY has the most number of ids, and most of the applications won't use this feature. So we load
     * inaccessible groups and filter to save time. (redis benchmark shows SMEMBERS of a 10K set takes 200ms at 99%, SUNION is worse)
     */
    @Override
    public Set<ParentReference> filterParentsByAppId(Set<ParentReference> parentReferences, String partitionId, String appId) {
        RedisConnectionPool connectionPool = this.redisConnector.getPartitionRedisConnectionPool(partitionId);
        StatefulRedisConnection<String, String> connection = connectionPool.getConnection();
        try {
            RedisCommands<String, String> commands = connection.sync();
            commands.select(config.getPartitionAppId());
            KeyScanCursor<String> cursor = commands.scan(ScanArgs.Builder.limit(200));
            List<String> allAppIds = cursor.getKeys();
            while (!cursor.isFinished()) {
                cursor = commands.scan(cursor);
                allAppIds.addAll(cursor.getKeys());
            }
            allAppIds.removeAll(Arrays.asList(DEFAULT_APPID_KEY, appId));
            if (allAppIds.isEmpty()) {
                return parentReferences;
            }
            Set<String> inAccessibleGroups = commands.sunion(allAppIds.toArray(new String[allAppIds.size()]));
            Set<String> accessibleGroups = commands.smembers(appId);
            inAccessibleGroups.removeAll(accessibleGroups);
            return parentReferences.stream().filter(r -> !inAccessibleGroups.contains(r.getId())).collect(Collectors.toSet());

        } finally {
            connectionPool.returnConnection(connection, log);
        }
    }
}
