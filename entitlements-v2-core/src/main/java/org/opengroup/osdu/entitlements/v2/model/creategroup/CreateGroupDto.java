package org.opengroup.osdu.entitlements.v2.model.creategroup;

import com.google.common.base.Strings;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupDto {
    @Pattern(regexp = "^[A-Za-z0-9{}_.-]{3,128}$")
    @NotNull
    private String name;
    @Length(min = 0, max = 256)
    private String description;

    public static EntityNode createGroupNode(CreateGroupDto dto, String domain, String partitionId) {
        return EntityNode.builder()
                .name(dto.name.toLowerCase())
                .nodeId(String.format("%s@%s", dto.name.toLowerCase(), domain.toLowerCase()))
                .description(Strings.isNullOrEmpty(dto.description) ? "" : dto.description)
                .type(NodeType.GROUP)
                .dataPartitionId(partitionId)
                .build();
    }
}
