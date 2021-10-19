package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.helper;

import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper.BasicMongoDBHelper;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.UserDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.IdDoc;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements.entity.internal.NodeRelationDoc;
import org.opengroup.osdu.entitlements.v2.aws.util.ExceptionGenerator;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.DIRECT_PARENTS;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.ID;
import static org.opengroup.osdu.entitlements.v2.aws.spi.BasicEntitlementsHelper.MEMBER_OF;


/**
 * Common-used operations with GroupDoc
 */
@Component
public class UserHelper extends NodeHelper {

    @PostConstruct
    //TODO: recheck indexes
    public void init() {
        helper.ensureIndex(UserDoc.class, new Index().on("_id.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("_id.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("memberOf", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("memberOf.role", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("memberOf.parentId", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("memberOf.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("memberOf.parentId.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("directParents", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("directParents.role", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("directParents.parentId", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("directParents.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(UserDoc.class, new Index().on("directParents.parentId.dataPartitionId", Sort.Direction.ASC));
    }

    @Autowired
    public UserHelper(BasicMongoDBHelper helper) {
        super(helper);
    }

    public UserDoc getOrCreate(UserDoc docForCheck) {
        UserDoc userDoc = getById(docForCheck.getId());
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
        helper.insert(userDoc);
    }

    public UserDoc getById(IdDoc id) {
        if (id == null) {
            throw ExceptionGenerator.idIsNull();
        }
        return helper.getById(id, UserDoc.class);
    }

    public void addDirectRelation(IdDoc userId, NodeRelationDoc parentRelation) {
        helper.updateMulti(
                Query.query(Criteria.where(ID).is(userId)),
                new Update().addToSet(DIRECT_PARENTS, parentRelation),
                UserDoc.class);
    }

    public void addMemberRelations(IdDoc userId, Set<NodeRelationDoc> addedParents) {
        helper.updateMulti(
                Query.query(Criteria.where(ID).is(userId)),
                new Update().addToSet(MEMBER_OF).each(addedParents),
                UserDoc.class);
    }

    //TODO: recheck duplicating
    public void addMemberRelations(Set<IdDoc> userIds, Set<NodeRelationDoc> addedParents) {
        helper.updateMulti(
                Query.query(Criteria.where(ID).in(userIds)),
                new Update().addToSet(MEMBER_OF).each(addedParents),
                UserDoc.class);
    }

    public List<UserDoc> getUsers(Collection<String> nodeIds, String partitionId) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyList();
        }
        Collection<IdDoc> ids = nodeIds.stream()
                .map(nodeId -> new IdDoc(nodeId, partitionId))
                .collect(Collectors.toList());
        return helper.find(Query.query(Criteria.where(ID).in(ids)), UserDoc.class);
    }

    public Set<IdDoc> getAllChildUsers(IdDoc parentGroup) {
        AggregationOperation match = Aggregation.match(Criteria.where("memberOf.parentId").is(parentGroup));

        //Important to have projection to get just IdDoc instead of full UserDoc set because of unpredictable count of users.
        AggregationOperation project = Aggregation.project()
                .and("id.nodeId").as("nodeId")
                .and("id.dataPartitionId").as("dataPartitionId")
                .andExclude("_id");
        Aggregation aggregation = Aggregation.newAggregation(match, project);
        AggregationResults<IdDoc> results = helper.pipeline(aggregation, UserDoc.class, IdDoc.class);
        return new HashSet<>(results.getMappedResults());
    }

    public Set<ChildrenReference> getDirectChildren(IdDoc parentGroup) {
        return super.getDirectChildren(parentGroup, UserDoc.class);
    }

    public boolean checkDirectParent(IdDoc userToCheckParent, NodeRelationDoc relationForCheck) {
        return super.checkDirectParent(userToCheckParent, relationForCheck, UserDoc.class);
    }

    public void rewriteMemberOfRelations(IdDoc userId, Set<NodeRelationDoc> userRelations) {
        helper.update(UserDoc.class)
                .matching(Criteria.where(ID).is(userId))
                .apply(new Update().set("memberOf", userRelations))
                .first();
    }

    public void removeDirectParentRelation(IdDoc userToRemoveParent, IdDoc groupToRemoveFromParents) {
        super.removeDirectParentRelation(userToRemoveParent, groupToRemoveFromParents, UserDoc.class);
    }
}
