package org.opengroup.osdu.entitlements.v2.model.listgroup;

import com.dslplatform.json.CompiledJson;
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
public class ListGroupResponseDto {
    private String desId;
    private String memberEmail;
    private List<ParentReference> groups;
}
