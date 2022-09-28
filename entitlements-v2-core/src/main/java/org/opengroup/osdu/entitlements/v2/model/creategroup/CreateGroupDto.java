package org.opengroup.osdu.entitlements.v2.model.creategroup;

import com.google.common.base.Strings;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupDto {
    // TODO: These should be moved to org.opengroup.osdu.core.common.model.storage.validation - ValidationDoc
    //       constants once we decide on our validation regexes at the forum level (and made public).
    private static final String NAME_REGEX = "^[A-Za-z0-9{}_.-]{3,128}$";
    private static final String FREE_TEXT_REGEX = "^[A-Za-z0-9 _.-/,;:\'\"!@&+%#$]{0,255}$";

    @Pattern(regexp = NAME_REGEX)
    @NotNull
    private String name;

    @Pattern(regexp = FREE_TEXT_REGEX)
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