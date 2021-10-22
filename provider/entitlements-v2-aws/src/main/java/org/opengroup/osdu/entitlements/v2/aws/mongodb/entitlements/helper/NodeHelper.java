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


import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper.BasicMongoDBHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.HashSet;
import java.util.Set;

import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.DIRECT_PARENTS;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.ID;


public abstract class NodeHelper {

    protected final BasicMongoDBHelper helper;
    protected final IndexUpdater indexUpdater;

    public NodeHelper(BasicMongoDBHelper helper, IndexUpdater indexUpdater) {
        this.helper = helper;
        this.indexUpdater = indexUpdater;
    }

    //TODO: check performance limitations and rewrite if necessary.
    protected Set<ChildrenReference> getDirectChildren(IdDoc parentGroup, String collectionName) {
        AggregationOperation match = Aggregation.match(
                Criteria.where(DIRECT_PARENTS).elemMatch(
                        Criteria.where("parentId").is(parentGroup)
                )
        );
        AggregationOperation project = Aggregation.project()
                .and("directParents").as("directParents")
                .and("_id.nodeId").as("nodeId")
                .and("_id.dataPartitionId").as("dataPartitionId")
                .andExclude("_id");
        AggregationOperation unwind = Aggregation.unwind("directParents");
        AggregationOperation matchAfterUnwind = Aggregation.match(
                Criteria.where("directParents.parentId.dataPartitionId").is(parentGroup.getDataPartitionId())
                        .and("directParents.parentId.nodeId").is(parentGroup.getNodeId())
        );
        AggregationOperation projectAfterUnwind = Aggregation.project()
                .and("nodeId").as("_id")
                .and("dataPartitionId").as("dataPartitionId")
                .and("directParents.role").as("role");
        Aggregation aggregation = Aggregation.newAggregation(match, project, unwind, matchAfterUnwind, projectAfterUnwind);
        AggregationResults<ChildrenReference> results = helper.pipeline(aggregation, collectionName, ChildrenReference.class);
        return new HashSet<>(results.getMappedResults());
    }

    public boolean checkDirectParent(IdDoc nodeToCheckParent, NodeRelationDoc relationForCheck, String collectionName) {
        return helper.existsByQuery(
                Query.query(Criteria.where(ID).is(nodeToCheckParent).and(DIRECT_PARENTS).is(relationForCheck)),
                collectionName
        );
    }

    public void removeDirectParentRelation(IdDoc nodeToRemoveParent, IdDoc groupToRemoveFromParents, Class<?> clazz, String collectionName) {
        helper.update(clazz, collectionName)
                .matching(Query.query(Criteria.where(ID).is(nodeToRemoveParent)))
                .apply(
                        new Update().pull(
                                DIRECT_PARENTS,
                                Query.query(Criteria.where("parentId").is(groupToRemoveFromParents))
                        )
                )
                .first();
    }

    protected void removeAllDirectChildrenRelations(IdDoc parentNode, Class<?> clazz, String collectionName) {
        helper.update(clazz, collectionName)
                .matching(
                        Query.query(
                                Criteria.where(DIRECT_PARENTS).elemMatch(
                                        Criteria.where("parentId").is(parentNode)
                                )
                        )
                )
                .apply(
                        new Update().pull(
                                DIRECT_PARENTS,
                                Query.query(Criteria.where("parentId").is(parentNode)
                                )
                        )
                )
                .all();
    }

}
