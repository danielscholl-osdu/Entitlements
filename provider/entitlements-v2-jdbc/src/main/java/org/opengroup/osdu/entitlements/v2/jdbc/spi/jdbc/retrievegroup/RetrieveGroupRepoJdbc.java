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

package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.retrievegroup;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.jdbc.JdbcAppProperties;
import org.opengroup.osdu.entitlements.v2.jdbc.exception.DatabaseAccessException;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.model.MemberInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.model.*;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RetrieveGroupRepoJdbc implements RetrieveGroupRepo {

    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final JdbcTemplateRunner jdbcTemplateRunner;
    private final JaxRsDpsLog log;
    private final JdbcAppProperties config;

    @Override
    public EntityNode groupExistenceValidation(String groupId, String partitionId) {
        Optional<EntityNode> groupNode = getEntityNode(groupId, partitionId);
        return groupNode.orElseThrow(() -> {
            log.info(String.format("Can't find group %s", groupId));
            return new DatabaseAccessException(
                    HttpStatus.NOT_FOUND,
                    String.format("Group %s is not found", groupId));
        });
    }

    @Override
    public Optional<EntityNode> getEntityNode(String entityEmail, String partitionId) {
        Optional<GroupInfoEntity> groupInfoEntity = groupRepository.findByEmail(entityEmail).stream()
                .findFirst();

        if (groupInfoEntity.isPresent()){
            return Optional.of(groupInfoEntity.get().toEntityNode());
        } else {
            log.warning("Could not find the group with email: " + entityEmail);
            return Optional.empty();
        }
    }

    @Override
    public EntityNode getMemberNodeForRemovalFromGroup(String memberId, String partitionId) {
        if (!memberId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))) {
            return EntityNode.createMemberNodeForNewUser(memberId, partitionId);
        }
        return EntityNode.createNodeFromGroupEmail(memberId);
    }

    //Left without implementation as not necessary for provider
    @Override
    public Set<EntityNode> getEntityNodes(String partitionId, List<String> nodeIds) {
        return Collections.emptySet();
    }

    //Left without implementation as not necessary for provider
    @Override
    public Map<String, Set<String>> getUserPartitionAssociations(Set<String> userIds) {
        return Collections.emptyMap();
    }

    //Left without implementation as not necessary for provider
    @Override
    public Set<EntityNode> getAllGroupNodes(String partitionId, String partitionDomain) {
        return Collections.emptySet();
    }

    @Override
    public Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference) {
        GroupInfoEntity parent = groupRepository.findByName(groupNode.getName()).stream()
                .findFirst()
                .orElseThrow(() -> DatabaseAccessException.createNotFound(groupNode.getNodeId()));

        return childrenReference.isGroup() ?
                hasChildGroupInGroup(parent, childrenReference) :
                hasMemberInGroup(parent, childrenReference);
    }

    @Override
    public List<ParentReference> loadDirectParents(String partitionId, String... nodeId) {
        //The method is used in JDBC module only for members and does not provide a solution
        // to identify the type of the node.
        //Should be reworked later
        List<Long> childrenIds = memberRepository.findByEmail(nodeId[0]).stream()
                .map(MemberInfoEntity::getId)
                .collect(Collectors.toList());

        return groupRepository.findDirectGroups(childrenIds).stream()
                .map(GroupInfoEntity::toParentReference)
                .collect(Collectors.toList());
    }

    @Override
    public ParentTreeDto loadAllParents(EntityNode memberNode) {
        List<Long> parentIds = jdbcTemplateRunner.getRecursiveParentIds(memberNode);
        List<GroupInfoEntity> parentGroupEntities = (List<GroupInfoEntity>) groupRepository.findAllById(parentIds);

        String partitionId = Optional.ofNullable(memberNode.getDataPartitionId()).orElse(StringUtils.EMPTY);
        Set<ParentReference> parents = parentGroupEntities.stream()
                .filter(parentGroup -> partitionId.equalsIgnoreCase(parentGroup.getPartitionId()))
                .map(GroupInfoEntity::toParentReference)
                .collect(Collectors.toSet());

        return ParentTreeDto.builder().parentReferences(parents).build();
    }

    @Override
    public List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeId) {
        //The method is not used in JDBC module and does not provide a solution
        // to identify the type of the node.
        //Should be reworked later
        List<Long> parentIds = groupRepository.findByEmail(nodeId[0]).stream()
                .map(GroupInfoEntity::getId)
                .collect(Collectors.toList());

        List<ChildrenReference> children = groupRepository.findDirectChildren(parentIds).stream()
                .map(GroupInfoEntity::toChildrenReference)
                .collect(Collectors.toList());
        List<ChildrenReference> members = parentIds.stream()
                .flatMap(id -> memberRepository.findMembersByGroup(id).stream())
                .map(MemberInfoEntity::toChildrenReference)
                .collect(Collectors.toList());

        children.addAll(members);
        return children;
    }

    @Override
    public ChildrenTreeDto loadAllChildrenUsers(EntityNode node) {
        return ChildrenTreeDto.builder().childrenUserIds(Collections.emptyList()).build();
    }

    @Override
    public Set<ParentReference> filterParentsByAppId(Set<ParentReference> parentReferences,
                                                     String partitionId, String appId) {
        return parentReferences.stream()
                .filter(pr -> pr.getAppIds().isEmpty() || pr.getAppIds().contains(appId))
                .collect(Collectors.toSet());
    }

    //Left without implementation as not necessary for provider
    @Override
    public Set<String> getGroupOwners(String partitionId, String nodeId) {
        return Collections.emptySet();
    }

    //Left without implementation as not necessary for provider
    @Override
    public Map<String, Integer> getAssociationCount(List<String> userIds) {
        return Collections.emptyMap();
    }

    //Left without implementation as not necessary for provider
    @Override
    public Map<String, Integer> getAllUserPartitionAssociations() {
        return Collections.emptyMap();
    }


    private boolean hasMemberInGroup(GroupInfoEntity parent, ChildrenReference childrenReference){
        return !memberRepository.findMemberByEmailInGroup(parent.getId(), childrenReference.getId()).isEmpty();
    }

    private boolean hasChildGroupInGroup(GroupInfoEntity parent, ChildrenReference childrenReference) {
        return !groupRepository.findChildByEmail(parent.getId(), childrenReference.getId()).isEmpty();
    }
}
