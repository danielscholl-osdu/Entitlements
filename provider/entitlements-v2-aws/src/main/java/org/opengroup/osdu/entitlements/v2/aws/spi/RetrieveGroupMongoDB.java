/**
* Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.entitlements.v2.aws.spi;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.NotImplementedException;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.aws.AwsAppProperties;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupsOfPartitionDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class RetrieveGroupMongoDB extends BasicEntitlementsHelper implements RetrieveGroupRepo {

    @Autowired
    private AwsAppProperties config;

    private Function<GroupDoc, EntityNode> groupToEntityNode;
    private Function<UserDoc, EntityNode> userToEntityNode;

    @PostConstruct
    private void init() {
        groupToEntityNode = (doc -> conversionService.convert(doc, EntityNode.class));
        userToEntityNode = (doc -> conversionService.convert(doc, EntityNode.class));
    }

    @Override
    public EntityNode groupExistenceValidation(String groupId, String partitionId) {
        GroupDoc groupToCheck = groupHelper.getById(new IdDoc(groupId, partitionId));
        if (groupToCheck == null) {
            throw new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), String.format("Group %s is not found", groupId));
        }
        return conversionService.convert(groupHelper.getById(new IdDoc(groupId, partitionId)), EntityNode.class);
    }

    @Override
    public Optional<EntityNode> getEntityNode(String entityEmail, String partitionId) {

        EntityNode node = Optional.ofNullable(groupHelper.getById(new IdDoc(entityEmail, partitionId))).map(groupToEntityNode)
                .orElseGet(() -> conversionService.convert(userHelper.getById(new IdDoc(entityEmail, partitionId)), EntityNode.class));

        return Optional.ofNullable(node);
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
        Collection<IdDoc> groupIds = nodeIds.stream()
                .map(nodeId -> new IdDoc(nodeId, partitionId))
                .collect(Collectors.toSet());
        Set<EntityNode> groups = groupHelper.getGroups(groupIds).stream()
                .map(groupToEntityNode)
                .collect(Collectors.toSet());
        Set<EntityNode> users = userHelper.getUsers(nodeIds, partitionId).stream()
                .map(userToEntityNode)
                .collect(Collectors.toSet());
        groups.addAll(users);
        return groups;
    }

    @Override
    public Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference) {

        NodeRelationDoc relationForCheck = NodeRelationDoc.builder()
                .parentId(new IdDoc(groupNode.getNodeId(), groupNode.getDataPartitionId()))
                .role(childrenReference.getRole())
                .build();
        IdDoc nodeToCheckParent = new IdDoc(childrenReference.getId(), childrenReference.getDataPartitionId());

        if (childrenReference.getType() == NodeType.USER) {
            return userHelper.checkDirectParent(nodeToCheckParent, relationForCheck);
        }
        if (childrenReference.getType() == NodeType.GROUP) {
            return groupHelper.checkDirectParent(nodeToCheckParent, relationForCheck);
        }
        throw new AppException(401,"Unknown NodeType: " + childrenReference.getType(), "Error getting node type" );
    }


    @Override
    public List<ParentReference> loadDirectParents(String partitionId, String... nodeIds) {

        List<ParentReference> parentReferences = new ArrayList<>();
        for (String nodeId : nodeIds) {
            if (!nodeId.endsWith(String.format("@%s.%s", partitionId, config.getDomain()))) { //it is a user not a group
                parentReferences.addAll(userHelper.loadDirectParents(new IdDoc(nodeId, partitionId)));
            }
            else { //its a group
                parentReferences.addAll(groupHelper.loadDirectParents(new IdDoc(nodeId, partitionId)));
            }
        }
        return parentReferences;
    }

    @Override
    public ParentTreeDto loadAllParents(EntityNode memberNode) {
        if (memberNode.getType() == NodeType.GROUP) {
            GroupDoc groupToCheckParents = groupHelper.getById(new IdDoc(memberNode.getNodeId(), memberNode.getDataPartitionId()));
            Set<GroupDoc> parentGroups = groupHelper.loadAllParents(Sets.newHashSet(groupToCheckParents));

            Set<ParentReference> parentReferences = new HashSet<>();
            addParentReferences(parentGroups, parentReferences);
            return ParentTreeDto.builder()
                    .parentReferences(parentReferences)
                    .build();
        }
        if (memberNode.getType() == NodeType.USER) {
            UserDoc userToCheckParents = userHelper.getById(new IdDoc(memberNode.getNodeId(), memberNode.getDataPartitionId()));
            if (userToCheckParents == null) {
                return ParentTreeDto.builder().parentReferences(new HashSet<>()).build();
            }
            Set<GroupDoc> allParentIDs = userToCheckParents.getAllParents().stream()
                .map(NodeRelationDoc::getParentId)
                .map(groupHelper::getById)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            Set<GroupDoc> allParentGroups = groupHelper.loadAllParents(allParentIDs);

            Set<ParentReference> parentReferences = new HashSet<>();
            addParentReferences(allParentGroups, parentReferences); // Add parents of parents (or more)
            addParentReferences(allParentIDs, parentReferences);    // Add direct parents
            return ParentTreeDto.builder()
                    .parentReferences(parentReferences)
                    .build();
        }
        throw new AppException(401,"Unknown NodeType: memberNode.getType() ", "Error getting node type" );
    }

    @Override
    public Set<ParentReference> filterParentsByAppId(Set<ParentReference> parentReferences, String partitionId, String appId) {
        return parentReferences.stream()
                .filter(pr -> pr.getAppIds().isEmpty() || pr.getAppIds().contains(appId))
                .collect(Collectors.toSet());
    }

    private void addParentReferences(Collection<GroupDoc> groupsToConvert, Set<ParentReference> parentReferenceToFill) {
        for (GroupDoc group : groupsToConvert) {
            parentReferenceToFill.add(
                    ParentReference.builder()
                            .id(group.getId().getNodeId())
                            .dataPartitionId(group.getId().getDataPartitionId())
                            .name(group.getName())
                            .description(group.getDescription())
                            .appIds(group.getAppIds())
                            .build()
            );
        }
    }

    @Override
    public Set<String> getGroupOwners(String partitionId, String nodeId) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, Integer> getAssociationCount(List<String> userIds) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, Integer> getAllUserPartitionAssociations() {
        throw new NotImplementedException();
    }

    @Override
    public ListGroupsOfPartitionDto getGroupsInPartition(String dataPartitionId, GroupType groupType, String cursor, Integer limit) {
        List<GroupDoc> groupDocs = groupHelper.getGroupsByPartitionId(dataPartitionId, groupType, cursor, limit);
        List<ParentReference> groups = groupDocs.stream()
            .map(groupDoc -> ParentReference.builder()
                .id(groupDoc.getId().getNodeId())
                .dataPartitionId(groupDoc.getId().getDataPartitionId())
                .name(groupDoc.getName())
                .description(groupDoc.getDescription())
                .appIds(groupDoc.getAppIds())
                .build())
            .collect(Collectors.toSet()) // Collect into a Set to remove duplicates
            .stream() // Stream the Set
            .collect(Collectors.toList()); // Collect back into a List

        Long totalCount = (long) groups.size();
        
        return ListGroupsOfPartitionDto.builder()
                .groups( groups)
                .totalCount(totalCount)
                .cursor(cursor)
                .build();
        
    }

    @Override
    public ChildrenTreeDto loadAllChildrenUsers(EntityNode node) {
        throw new NotImplementedException();
    }

    @Override
    public List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeId) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, Set<String>> getUserPartitionAssociations(Set<String> userIds) {
        throw new NotImplementedException();
    }

    @Override
    public Set<EntityNode> getAllGroupNodes(String partitionId, String partitionDomain) {
        throw new NotImplementedException();
    }

}
