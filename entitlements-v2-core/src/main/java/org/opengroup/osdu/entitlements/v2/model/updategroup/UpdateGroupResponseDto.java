package org.opengroup.osdu.entitlements.v2.model.updategroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@Generated
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Represents a model for the Update Group Response")
public class UpdateGroupResponseDto {

    @Schema(description = "Name of the Updated Group")
    private String name;

    @Schema(description = "Email of the Updated Group")
    private String email;

    @Schema(description = "List of AppId of the Updated Group")
    private List<String> appIds;
}
