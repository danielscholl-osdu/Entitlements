package org.opengroup.osdu.entitlements.v2.model.listgroup;

import com.dslplatform.json.CompiledJson;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;

import java.util.List;

@Data
@CompiledJson
@Generated
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Represents a model for List Group Response.")
public class ListGroupResponseDto {

    @Schema(description = "desId")
    private String desId;

    @Schema(description = "member email")
    private String memberEmail;

    @Schema(description = "Represents a List of Groups")
    private List<ParentReference> groups;
}
