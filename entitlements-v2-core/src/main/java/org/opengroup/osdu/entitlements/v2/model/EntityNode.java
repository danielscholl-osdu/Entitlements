package org.opengroup.osdu.entitlements.v2.model;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.JsonConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@CompiledJson
@Generated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityNode {

    private String nodeId;
    private NodeType type;
    private String name;
    @Builder.Default
    private String description = "";
    private String dataPartitionId;
    @Builder.Default
    private Set<String> appIds = new HashSet<>();

    public static EntityNodeBuilder builder() {
        return new EntityNodeBuilder();
    }

    public static final int MAX_PARENTS = 5000;
    public static final String ROOT_DATA_GROUP_EMAIL_FORMAT = "users.data.root@%s";

    @JsonIgnore
    public String getUniqueIdentifier() {
        return String.format("%s-%s", nodeId, dataPartitionId);
    }

    @JsonAttribute(index = 4)
    public String getNodeId() {
        return nodeId;
    }

    @JsonAttribute(index = 5)
    public NodeType getType() {
        return type;
    }

    @JsonAttribute(index = 2)
    public String getName() {
        return name;
    }

    @JsonAttribute(index = 3)
    public String getDescription() {
        return description;
    }

    @JsonAttribute(index = 6)
    public String getDataPartitionId() {
        return dataPartitionId;
    }

    @JsonAttribute(index = 1)
    public Set<String> getAppIds() {
        return appIds;
    }

    public static EntityNode createMemberNodeForRequester(String requesterId, String partitionId) {
        return EntityNode.builder()
                .nodeId(requesterId.toLowerCase())
                .type(NodeType.USER)
                .name(requesterId.toLowerCase())
                .dataPartitionId(partitionId)
                .build();
    }

    public static EntityNode createMemberNodeForNewUser(String memberId, String partitionId) {
        return EntityNode.builder()
                .type(NodeType.USER)
                .nodeId(memberId.toLowerCase())
                .name(memberId.toLowerCase())
                .dataPartitionId(partitionId)
                .description(memberId)
                .build();
    }

    public static EntityNode createNodeFromGroupEmail(String email) {
        String[] emailParts = email.split("@");
        String[] domainParts = emailParts[1].split("\\.");
        return EntityNode.builder()
                .type(NodeType.GROUP)
                .nodeId(email.toLowerCase())
                .name(emailParts[0].toLowerCase())
                .dataPartitionId(domainParts[0])
                .build();
    }

    public static EntityNode createNodeFromParentReference(ParentReference ref) {
        return EntityNode.builder()
                .nodeId(ref.getId())
                .name(ref.getName())
                .description(ref.getDescription())
                .type(NodeType.GROUP)
                .dataPartitionId(ref.getDataPartitionId())
                .build();
    }

    public static Set<EntityNode> convertMemberNodeListFromListOfJson(List<String> memberNodeJsons) {
        Set<EntityNode> entityNodeList = new HashSet<>();
        memberNodeJsons.forEach(json -> entityNodeList.add(JsonConverter.fromJson(json, EntityNode.class)));
        return entityNodeList;
    }

    public void copyNode(EntityNode node) {
        this.setNodeId(node.getNodeId());
        this.setType(node.getType());
        this.setName(node.getName());
        this.setDataPartitionId(node.getDataPartitionId());
        this.setAppIds(node.getAppIds());
        this.setDescription(node.getDescription());
    }

    public Optional<ChildrenReference> getDirectChildReference(RetrieveGroupRepo retrieveGroupRepo, EntityNode groupNode) {
        ChildrenReference ownerRef = ChildrenReference.createChildrenReference(this, Role.OWNER);
        if (Boolean.TRUE.equals(retrieveGroupRepo.hasDirectChild(groupNode, ownerRef))) {
            return Optional.of(ownerRef);
        }
        ChildrenReference memberRef = ChildrenReference.createChildrenReference(this, Role.MEMBER);
        if (Boolean.TRUE.equals(retrieveGroupRepo.hasDirectChild(groupNode, memberRef))) {
            return Optional.of(memberRef);
        }
        return Optional.ofNullable(null);
    }

    public boolean isGroup() {
        return type == NodeType.GROUP;
    }

    public boolean isUser() {
        return type == NodeType.USER;
    }

    public boolean isDataGroup() {
        return isGroup() && name.toLowerCase().startsWith("data.");
    }

    public boolean crossPartitionAllowedGroup() {
        return isDataGroup() || isRootUsersGroup() || isDEPermissionGroup();
    }

    /*
    sharing user group is not considered as users group, we should not add the root user group as member when creating them
     */
    public boolean isUserGroup() {
        String lowercaseGroupName = name.toLowerCase();
        return isGroup() && (lowercaseGroupName.startsWith("users.") || lowercaseGroupName.startsWith("user.") || isRootUsersGroup()) &&
                !lowercaseGroupName.startsWith("users.sharing_");
    }

    public boolean isServiceGroup() {
        return isGroup() && name.toLowerCase().startsWith("service.");
    }

    public boolean isOtherGroup() {
        return isGroup() && !isServiceGroup() && !isDataGroup() && !isUserGroup();
    }

    public boolean isRootUsersGroup() {
        return isGroup() && "users".equalsIgnoreCase(name);
    }

    public boolean isDEPermissionGroup() {
        return isGroup() && name.startsWith("users.datalake.");
    }

    public boolean isUsersDataRootGroup() {
        return isGroup() && name.equalsIgnoreCase("users.data.root");
    }
}
