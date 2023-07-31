package org.opengroup.osdu.entitlements.v2.model;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

@Data
@CompiledJson
@Generated
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildrenReference {

    private String id;
    private String dataPartitionId;
    private NodeType type;
    private Role role;

    @JsonAttribute(index = 1)
    public Role getRole() {
        return role;
    }

    @JsonAttribute(index = 2)
    public String getId() {
        return id;
    }

    @JsonAttribute(index = 3)
    public String getDataPartitionId() {
        return dataPartitionId;
    }

    @JsonAttribute(index = 4)
    public NodeType getType() {
        return type;
    }

    public static ChildrenReference createChildrenReference(EntityNode memberNode, Role role) {
        return ChildrenReference.builder()
                .id(memberNode.getNodeId())
                .type(memberNode.getType())
                .role(role)
                .dataPartitionId(memberNode.getDataPartitionId())
                .build();
    }

    public static ChildrenReferenceBuilder builder(){
        return new ChildrenReferenceBuilder();
    }

    public boolean isGroup() {
        return type == NodeType.GROUP;
    }

    public boolean isUser() {
        return type == NodeType.USER;
    }

    public boolean isOwner() {
        return role == Role.OWNER;
    }

    public boolean isUsersDataRootGroup() {
        return id.startsWith("users.data.root@");
    }
}
