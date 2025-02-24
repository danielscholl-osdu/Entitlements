package org.opengroup.osdu.entitlements.v2.model.listmember;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Represents a model for the Member")
public class MemberDto {

    @Schema(description = "Email Id of the member")
    String email;

    @Schema(description = "Role of the member")
    Role role;

    @Schema(description = "Type of the member")
    @Builder.Default
    NodeType memberType = null;

    @Schema(description = "dataPartitionId")
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
