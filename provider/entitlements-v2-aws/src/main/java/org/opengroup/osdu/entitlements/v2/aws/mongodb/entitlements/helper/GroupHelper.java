// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper;

import com.google.common.collect.Sets;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper.BasicMongoDBHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.GroupDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.aws.util.ExceptionGenerator;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.APP_IDS;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.DIRECT_PARENTS;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.ID;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.NAME;


/**
 * Common-used operations with GroupDoc
 */
@Component
public class GroupHelper extends NodeHelper {

    @Autowired
    public GroupHelper(BasicMongoDBHelper helper, IndexUpdater indexUpdater) {
        super(helper, indexUpdater);
    }

    public void save(GroupDoc groupDoc) {
        if (groupDoc == null) {
            throw ExceptionGenerator.groupIsNull();
        }
        helper.insert(groupDoc, getGroupCollection(groupDoc.getId().getDataPartitionId()));
    }

    public GroupDoc getById(IdDoc groupId) {
        if (groupId == null) {
            throw ExceptionGenerator.idIsNull();
        }
        return helper.getById(groupId, GroupDoc.class, getGroupCollection(groupId.getDataPartitionId()));
    }

    public Set<NodeRelationDoc> getAllParentRelations(GroupDoc group) {
        Set<NodeRelationDoc> results = new HashSet<>();
        getParentRelations(Sets.newHashSet(group), results);
        return results;
    }

    private void getParentRelations(Collection<GroupDoc> groups, Set<NodeRelationDoc> resultsAccumulator) {

        Collection<IdDoc> groupIDsToProcess = new HashSet<>();
        for (GroupDoc group : groups) {
            Collection<NodeRelationDoc> relationsToProcess = group.getDirectParents();
            groupIDsToProcess.addAll(relationsToProcess.stream().map(NodeRelationDoc::getParentId).collect(Collectors.toSet()));
            resultsAccumulator.addAll(relationsToProcess);
        }

        if (!groupIDsToProcess.isEmpty()) {
            Collection<GroupDoc> parentGroupsToProcess = this.getGroups(groupIDsToProcess);
            this.getParentRelations(parentGroupsToProcess, resultsAccumulator);
        }
    }

    public Set<GroupDoc> loadAllParents(Set<GroupDoc> groups) {
        Set<GroupDoc> results = new HashSet<>();
        getParents(Sets.newHashSet(groups), results);
        return results;
    }

    private void getParents(Collection<GroupDoc> groups, Set<GroupDoc> resultsAccumulator) {

        Collection<IdDoc> groupIDsToProcess = new HashSet<>();
        for (GroupDoc group : groups) {
            groupIDsToProcess.addAll(group.getDirectParents().stream().map(NodeRelationDoc::getParentId).collect(Collectors.toSet()));
        }

        if (!groupIDsToProcess.isEmpty()) {
            Collection<GroupDoc> parentGroupsToProcess = this.getGroups(groupIDsToProcess);
            resultsAccumulator.addAll(parentGroupsToProcess);
            this.getParents(parentGroupsToProcess, resultsAccumulator);
        }
    }

    public void addDirectRelation(IdDoc groupId, NodeRelationDoc parentRelation) {
        helper.updateMulti(
                Query.query(Criteria.where(ID).is(groupId)),
                new Update().addToSet(DIRECT_PARENTS, parentRelation),
                GroupDoc.class,
                getGroupCollection(groupId.getDataPartitionId()));
    }

    public void removeAllDirectChildrenRelations(IdDoc parentGroup) {
        helper.update(GroupDoc.class, getGroupCollection(parentGroup.getDataPartitionId()))
                .matching(
                        Query.query(
                                Criteria.where(DIRECT_PARENTS).elemMatch(
                                        Criteria.where("parentId").is(parentGroup)
                                )
                        )
                )
                .apply(
                        new Update().pull(
                                DIRECT_PARENTS,
                                Query.query(Criteria.where("parentId").is(parentGroup)
                                )
                        )
                )
                .all();
    }

    public List<GroupDoc> getGroups(Collection<IdDoc> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return helper.find(
                Query.query(Criteria.where(ID).in(ids)),
                GroupDoc.class,
                getGroupCollection(ids.stream().findAny().get().getDataPartitionId())
        );
    }

    public Set<ChildrenReference> getDirectChildren(IdDoc parentGroup) {
        return super.getDirectChildren(parentGroup, getGroupCollection(parentGroup.getDataPartitionId()));
    }

    public boolean checkDirectParent(IdDoc nodeToCheckParent, NodeRelationDoc relationForCheck) {
        return super.checkDirectParent(nodeToCheckParent, relationForCheck, getGroupCollection(nodeToCheckParent.getDataPartitionId()));
    }

    public void delete(IdDoc groupId) {
        helper.delete(ID, groupId, getGroupCollection(groupId.getDataPartitionId()));
    }

    public List<ParentReference> loadDirectParents(IdDoc groupId) {
        GroupDoc group = this.getById(groupId);
        Set<NodeRelationDoc> directRelations = group.getDirectParents();
        List<ParentReference> parentReferences = new ArrayList<>();
        for (NodeRelationDoc directRelation : directRelations) {
            parentReferences.add(ParentReference.builder()
                    .id(directRelation.getParentId().getNodeId())
                    .dataPartitionId(directRelation.getParentId().getDataPartitionId())
                    .build()
            );
        }
        return parentReferences;
    }

    public void removeDirectParentRelation(IdDoc groupToRemoveParent, IdDoc groupToRemoveFromParents) {
        super.removeDirectParentRelation(
                groupToRemoveParent,
                groupToRemoveFromParents,
                GroupDoc.class,
                getGroupCollection(groupToRemoveParent.getDataPartitionId()));
    }

    public void renameGroup(IdDoc groupToRename, String newGroupName) {
        Query query = Query.query(Criteria.where(ID).is(groupToRename));
        helper.updateMulti(
                query,
                Update.update(NAME, newGroupName),
                GroupDoc.class,
                getGroupCollection(groupToRename.getDataPartitionId())
        );
    }

    public void updateAppIds(EntityNode groupToUpdateIds, Set<String> allowedAppIds) {
        Query query = Query.query(Criteria.where(ID).in(groupToUpdateIds));
        helper.updateMulti(
                query,
                new Update().set(APP_IDS, allowedAppIds),
                GroupDoc.class,
                getGroupCollection(groupToUpdateIds.getDataPartitionId())
        );
    }

    private String getGroupCollection(String dataPartitionId) {
        indexUpdater.checkIndex(dataPartitionId);
        return "Group-" + dataPartitionId;
    }
}
