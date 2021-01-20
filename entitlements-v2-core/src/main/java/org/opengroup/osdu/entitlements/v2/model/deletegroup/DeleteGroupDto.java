package org.opengroup.osdu.entitlements.v2.model.deletegroup;


import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class DeleteGroupDto {
    private String groupEmail;

    public static EntityNode deleteGroupNode(DeleteGroupDto dto, String partitionId) {
        return EntityNode.builder()
                .name(dto.groupEmail.split("@")[0])
                .nodeId(dto.groupEmail.toLowerCase())
                .type(NodeType.GROUP)
                .dataPartitionId(partitionId)
                .build();
    }
}
