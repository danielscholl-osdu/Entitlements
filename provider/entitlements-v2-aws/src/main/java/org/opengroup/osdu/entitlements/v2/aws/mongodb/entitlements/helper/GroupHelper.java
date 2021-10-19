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
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
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

    @PostConstruct
    //TODO: recheck indexes
    public void init() {
        helper.ensureIndex(GroupDoc.class, new Index().on("_id.dataPartitionId", Sort.Direction.ASC));
        helper.ensureIndex(GroupDoc.class, new Index().on("_id.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(GroupDoc.class, new Index().on("directParents", Sort.Direction.ASC));
        helper.ensureIndex(GroupDoc.class, new Index().on("directParents.role", Sort.Direction.ASC));
        helper.ensureIndex(GroupDoc.class, new Index().on("directParents.parentId", Sort.Direction.ASC));
        helper.ensureIndex(GroupDoc.class, new Index().on("directParents.parentId.nodeId", Sort.Direction.ASC));
        helper.ensureIndex(GroupDoc.class, new Index().on("directParents.parentId.dataPartitionId", Sort.Direction.ASC));
    }

    @Autowired
    public GroupHelper(BasicMongoDBHelper helper) {
        super(helper);
    }

    public void save(GroupDoc groupDoc) {
        if (groupDoc == null) {
            throw ExceptionGenerator.groupIsNull();
        }
        helper.insert(groupDoc);
    }

    public GroupDoc getById(IdDoc groupId) {
        if (groupId == null) {
            throw ExceptionGenerator.idIsNull();
        }
        return helper.getById(groupId, GroupDoc.class);
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
                GroupDoc.class).getModifiedCount();
    }

    public void removeAllDirectChildrenRelations(IdDoc parentGroup) {
        helper.update(GroupDoc.class)
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
        return helper.find(Query.query(Criteria.where(ID).in(ids)), GroupDoc.class);
    }

    public Set<ChildrenReference> getDirectChildren(IdDoc parentGroup) {
        return super.getDirectChildren(parentGroup, GroupDoc.class);
    }

    public boolean checkDirectParent(IdDoc nodeToCheckParent, NodeRelationDoc relationForCheck) {
        return super.checkDirectParent(nodeToCheckParent, relationForCheck, GroupDoc.class);
    }

    public void delete(IdDoc groupId) {
        helper.delete(ID, groupId, GroupDoc.class);
    }

    public List<ParentReference> loadDirectParents(IdDoc groupId) {
        GroupDoc group = getById(groupId);
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
        super.removeDirectParentRelation(groupToRemoveParent, groupToRemoveFromParents, GroupDoc.class);
    }

    public void renameGroup(IdDoc groupToRename, String newGroupName) {
        Query query = Query.query(Criteria.where(ID).is(groupToRename));
        helper.updateMulti(query, Update.update(NAME, newGroupName), GroupDoc.class);
    }

    public void updateAppIds(EntityNode groupToUpdateIds, Set<String> allowedAppIds) {
        Query query = Query.query(Criteria.where(ID).in(groupToUpdateIds));
        helper.updateMulti(query, new Update().set(APP_IDS, allowedAppIds), GroupDoc.class);
    }
}
