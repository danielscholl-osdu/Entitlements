package org.opengroup.osdu.entitlements.v2.model.updategroup;

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
public class UpdateGroupResponseDto {
    private String name;
    private String email;
    private List<String> appIds;
}
