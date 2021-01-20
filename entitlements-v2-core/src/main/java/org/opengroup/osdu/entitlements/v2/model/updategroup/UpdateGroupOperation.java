package org.opengroup.osdu.entitlements.v2.model.updategroup;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class UpdateGroupOperation {
    @ValidUpdateGroupOp
    @JsonProperty("op")
    private String operation;

    @ValidUpdateGroupPath
    private String path;

    private List<String> value;
}
