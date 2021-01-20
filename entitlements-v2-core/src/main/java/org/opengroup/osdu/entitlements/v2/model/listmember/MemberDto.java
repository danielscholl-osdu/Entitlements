package org.opengroup.osdu.entitlements.v2.model.listmember;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

@Builder
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MemberDto {
    String email;
    Role role;
    @Builder.Default
    NodeType memberType = null;
    @Builder.Default
    String dataPartitionId = null;

    public static MemberDto create(ChildrenReference reference, boolean includeType) {
        if (Boolean.TRUE.equals(includeType)) {
            return MemberDto.builder()
                    .email(reference.getId())
                    .role(reference.getRole())
                    .memberType(reference.getType())
                    .dataPartitionId(NodeType.GROUP.equals(reference.getType()) ? reference.getDataPartitionId() : null)
                    .build();
        } else {
            return MemberDto.builder()
                    .email(reference.getId())
                    .role(reference.getRole())
                    .build();
        }
    }
}
