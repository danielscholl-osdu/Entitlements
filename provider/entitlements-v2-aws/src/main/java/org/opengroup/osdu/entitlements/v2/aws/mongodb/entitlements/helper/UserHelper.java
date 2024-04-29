/**
 * Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper;

import org.opengroup.osdu.core.aws.mongodb.helper.BasicMongoDBHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.aws.util.ExceptionGenerator;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import java.util.*;
import java.util.stream.Collectors;

import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.DIRECT_PARENTS;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.ID;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.MEMBER_OF;


/**
 * Common-used operations with GroupDoc
 */
@Component
public class UserHelper extends NodeHelper {

    @Autowired
    public UserHelper(BasicMongoDBHelper helper, IndexUpdater indexUpdater) {
        super(helper, indexUpdater);
    }

    public UserDoc getOrCreate(UserDoc docForCheck) {
        UserDoc userDoc = this.getById(docForCheck.getId());
        if (userDoc != null) {
            return userDoc;
        }
        save(docForCheck);
        return docForCheck;
    }

    public void save(UserDoc userDoc) {
        if (userDoc == null) {
            throw ExceptionGenerator.userIsNull();
        }
        helper.insert(userDoc, getUserCollection(userDoc.getId().getDataPartitionId()));
    }

    public UserDoc getById(IdDoc id) {
        if (id == null) {
            throw ExceptionGenerator.idIsNull();
        }
        return helper.getById(id, UserDoc.class, getUserCollection(id.getDataPartitionId()));
    }

    public void addDirectRelation(IdDoc userId, NodeRelationDoc parentRelation) {
        helper.updateMulti(
                Query.query(Criteria.where(ID).is(userId)),
                new Update().addToSet(DIRECT_PARENTS, parentRelation),
                UserDoc.class,
                getUserCollection(userId.getDataPartitionId()));
    }

    public void addMemberRelations(IdDoc userId, Set<NodeRelationDoc> addedParents) {
        helper.updateMulti(
                Query.query(Criteria.where(ID).is(userId)),
                new Update().addToSet(MEMBER_OF).each(addedParents),
                UserDoc.class,
                getUserCollection(userId.getDataPartitionId()));
    }

    //recheck duplicating
    public void addMemberRelations(Set<IdDoc> userIds, Set<NodeRelationDoc> addedParents) {
        if (userIds.isEmpty() || addedParents.isEmpty()) {
            return;
        }
        helper.updateMulti(
                Query.query(Criteria.where(ID).in(userIds)),
                new Update().addToSet(MEMBER_OF).each(addedParents),
                UserDoc.class,
                getUserCollection(userIds.stream().findAny().get().getDataPartitionId()));
    }

    public List<UserDoc> getUsers(Collection<String> nodeIds, String partitionId) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyList();
        }
        Collection<IdDoc> ids = nodeIds.stream()
                .map(nodeId -> new IdDoc(nodeId, partitionId))
                .collect(Collectors.toList());
        return helper.find(
                Query.query(Criteria.where(ID).in(ids)),
                UserDoc.class,
                getUserCollection(partitionId));
    }

    public Set<IdDoc> getAllChildUsers(IdDoc parentGroup) {
        AggregationOperation match = Aggregation.match(Criteria.where("memberOf.parentId").is(parentGroup));

        //Important to have projection to get just IdDoc instead of full UserDoc set because of unpredictable count of users.
        AggregationOperation project = Aggregation.project()
                .and("_id.nodeId").as("nodeId")
                .and("_id.dataPartitionId").as("dataPartitionId")
                .andExclude("_id");
        Aggregation aggregation = Aggregation.newAggregation(match, project);
        AggregationResults<IdDoc> results = helper.pipeline(
                aggregation,
                getUserCollection(parentGroup.getDataPartitionId()),
                IdDoc.class);
        return new HashSet<>(results.getMappedResults());
    }

    public Set<ChildrenReference> getDirectChildren(IdDoc parentGroup) {
        return super.getDirectChildren(parentGroup, getUserCollection(parentGroup.getDataPartitionId()));
    }

    public boolean checkDirectParent(IdDoc userToCheckParent, NodeRelationDoc relationForCheck) {
        return super.checkDirectParent(userToCheckParent, relationForCheck, getUserCollection(userToCheckParent.getDataPartitionId()));
    }

    public void rewriteMemberOfRelations(IdDoc userId, Set<NodeRelationDoc> userRelations) {
        helper.update(UserDoc.class, getUserCollection(userId.getDataPartitionId()))
                .matching(Criteria.where(ID).is(userId))
                .apply(new Update().set("memberOf", userRelations))
                .first();
    }

    public void removeAllDirectChildrenRelations(IdDoc parentGroup) {
        super.removeAllDirectChildrenRelations(parentGroup, UserDoc.class, getUserCollection(parentGroup.getDataPartitionId()));
    }

    public void removeDirectParentRelation(IdDoc userToRemoveParent, IdDoc groupToRemoveFromParents) {
        super.removeDirectParentRelation(
                userToRemoveParent,
                groupToRemoveFromParents,
                UserDoc.class,
                getUserCollection(userToRemoveParent.getDataPartitionId()));
    }

    private String getUserCollection(String dataPartitionId) {
        indexUpdater.checkIndex(dataPartitionId);
        return "User-" + dataPartitionId;
    }


    public List<ParentReference> loadDirectParents(IdDoc userId) {
        UserDoc user = this.getById(userId);
        Set<NodeRelationDoc> directRelations = user.getDirectParents();
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
}
