package org.opengroup.osdu.entitlements.v2.spi.retrievegroup;

import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupsOfPartitionDto;

public interface RetrieveGroupRepo {
    EntityNode groupExistenceValidation(String groupId, String partitionId);

    Optional<EntityNode> getEntityNode(String entityEmail, String partitionId);

    EntityNode getMemberNodeForRemovalFromGroup(String memberId, String partitionId);

    Set<EntityNode> getEntityNodes(String partitionId, List<String> nodeIds);

    Map<String, Set<String>> getUserPartitionAssociations(Set<String> userIds);

    Set<EntityNode> getAllGroupNodes(String partitionId, String partitionDomain);

    Boolean hasDirectChild(EntityNode groupNode, ChildrenReference childrenReference);

    List<ParentReference> loadDirectParents(String partitionId, String... nodeId);

    /**
     * Returns parents of given member, including from other data partitions.
     */
    ParentTreeDto loadAllParents(EntityNode memberNode);

    List<ChildrenReference> loadDirectChildren(String partitionId, String... nodeId);

    ChildrenTreeDto loadAllChildrenUsers(EntityNode node);

    Set<ParentReference> filterParentsByAppId(Set<ParentReference> parentReferences, String partitionId, String appId);

    Set<String> getGroupOwners(String partitionId, String nodeId);

    Map<String, Integer> getAssociationCount(List<String> userIds);

    Map<String, Integer> getAllUserPartitionAssociations();

    ListGroupsOfPartitionDto getGroupsInPartition(String dataPartitionId, GroupType groupType, String cursor, Integer limit);
}
