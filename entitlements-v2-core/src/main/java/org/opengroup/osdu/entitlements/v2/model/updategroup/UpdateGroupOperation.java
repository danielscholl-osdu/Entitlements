package org.opengroup.osdu.entitlements.v2.model.updategroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.entitlements.v2.validation.ValidUpdateGroupOp;
import org.opengroup.osdu.entitlements.v2.validation.ValidUpdateGroupPath;

import java.util.List;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a model for the Update Group Operation")
public class UpdateGroupOperation {
    @ValidUpdateGroupOp
    @JsonProperty("op")
    @Schema(description = "Update Group Operation")
    private String operation;

    @Schema(description = "Update Group Path")
    @ValidUpdateGroupPath
    private String path;

    @Schema(description = "list of values to be updated")
    private List<String> value;
}
