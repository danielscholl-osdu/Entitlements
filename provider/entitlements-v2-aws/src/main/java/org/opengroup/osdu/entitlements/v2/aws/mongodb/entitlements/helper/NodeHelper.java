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

    public NodeHelper(BasicMongoDBHelper helper) {
        this.helper = helper;
    }

    //TODO: check performance limitations and rewrite if necessary.
    protected Set<ChildrenReference> getDirectChildren(IdDoc parentGroup, Class<?> collection) {
        AggregationOperation match = Aggregation.match(
                Criteria.where(DIRECT_PARENTS).elemMatch(
                        Criteria.where("parentId").is(parentGroup)
                )
        );
        AggregationOperation project = Aggregation.project()
                .and("directParents").as("directParents")
                .and("id.nodeId").as("nodeId")
                .and("id.dataPartitionId").as("dataPartitionId")
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
        AggregationResults<ChildrenReference> results = helper.pipeline(aggregation, collection, ChildrenReference.class);
        return new HashSet<>(results.getMappedResults());
    }

    public boolean checkDirectParent(IdDoc nodeToCheckParent, NodeRelationDoc relationForCheck, Class<?> collection) {
        return helper.existsByQuery(
                Query.query(Criteria.where(ID).is(nodeToCheckParent).and(DIRECT_PARENTS).is(relationForCheck)),
                collection
        );
    }

    public void removeDirectParentRelation(IdDoc nodeToRemoveParent, IdDoc groupToRemoveFromParents, Class<?> collection) {
        helper.update(collection)
                .matching(Query.query(Criteria.where(ID).is(nodeToRemoveParent)))
                .apply(
                        new Update().pull(
                                DIRECT_PARENTS,
                                Query.query(Criteria.where("parentId").is(groupToRemoveFromParents))
                        )
                )
                .all();
    }


}
