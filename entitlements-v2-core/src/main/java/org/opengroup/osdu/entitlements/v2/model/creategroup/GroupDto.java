package org.opengroup.osdu.entitlements.v2.model.creategroup;

import com.dslplatform.json.CompiledJson;
import io.swagger.v3.oas.annotations.media.Schema;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import lombok.Data;
import lombok.Generated;

@Data
@CompiledJson
@Generated
@Schema(description = "Represents a model for the Group")
public class GroupDto {
    @Schema(description = "Name of the Group")
    String name;
    @Schema(description = "Email id of the Group")
    String email;
    @Schema(description = "Description of the Group")
    String description;

    public GroupDto() {
    }

    public GroupDto(String name, String email, String desc) {
        this.name = name;
        this.email = email;
        this.description = desc;
    }

    public static GroupDto createFromEntityNode(EntityNode node) {
        GroupDto dto = new GroupDto();
        dto.name = node.getName();
        dto.email = node.getNodeId();
        dto.description = node.getDescription();
        return dto;
    }
}
