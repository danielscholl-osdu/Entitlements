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
 *  limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.jdbc.service;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.jdbc.JdbcAppProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.config.properties.EntitlementsConfigurationProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.service.GroupCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupCacheServiceJdbc implements GroupCacheService {

    private final JdbcAppProperties config;

    private final EntitlementsConfigurationProperties configurationProperties;

    private final CacheLoader<EntityNode, Set<ParentReference>> cacheLoader;

    private LoadingCache<EntityNode, Set<ParentReference>> cache;

    private final RequestInfo requestInfo;

    private final JaxRsDpsLog log;

    private static final String CAN_IMPERSONATE = "users.datalake.delegation";

    private static final String CAN_BE_IMPERSONATED = "users.datalake.impersonation";

    @PostConstruct
    public void setUp() {
        cache = CacheBuilder.newBuilder()
            .expireAfterWrite(configurationProperties.getInMemoryCacheLifeSpan(), TimeUnit.SECONDS)
            .build(cacheLoader);
    }

    @Override
    public Set<ParentReference> getFromPartitionCache(String requesterId, String partitionId) {
        EntityNode requesterNode = getNodeByNodeType(requesterId, partitionId);
        Set<ParentReference> requesterGroups = cache.getUnchecked(requesterNode);
        String beneficialId = requestInfo.getHeaders().getOnBehalfOf();
        if (!Strings.isNullOrEmpty(beneficialId)) {
            if (requesterGroups.stream().map(ParentReference::getName).collect(Collectors.toList()).contains(CAN_IMPERSONATE)) {
                EntityNode beneficialNode = getNodeByNodeType(beneficialId, partitionId);
                Set<ParentReference> beneficialGroups = cache.getUnchecked(beneficialNode);
                Optional<ParentReference> group = beneficialGroups.stream().filter(item -> item.getName().equals(CAN_BE_IMPERSONATED)).findFirst();
                if (!group.isPresent()) {
                    log.warning("Impersonation group not found");
                    throw new AppException(HttpStatus.BAD_REQUEST.value(),
                            HttpStatus.BAD_REQUEST.getReasonPhrase(),
                            "Impersonation not allowed for " + beneficialId);
                } else {
                    return beneficialGroups;
                }
            } else {
                log.warning("Delegation group not found");
                throw new AppException(HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "Impersonation not allowed for " + requesterId);
            }
        }
        return requesterGroups;
    }

    @Override
    public void refreshListGroupCache(Set<String> userIds, String partitionId) {
        for (String userId : userIds) {
            flushListGroupCacheForUser(userId, partitionId);
        }
    }

    @Override
    public void flushListGroupCacheForUser(String userId, String partitionId) {
        EntityNode node = getNodeByNodeType(userId, partitionId);
        cache.invalidate(node);
    }

    private EntityNode getNodeByNodeType(String memberId, String partitionId) {
        return memberId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))
            ? EntityNode.createNodeFromGroupEmail(memberId)
            : EntityNode.createMemberNodeForNewUser(memberId, partitionId);
    }
}
