package org.opengroup.osdu.entitlements.v2.model.listgroup;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import org.opengroup.osdu.entitlements.v2.model.GroupType;

@Data
@Generated
@Builder
public class ListGroupOnBehalfOfServiceDto {
    private String memberId;
    private GroupType groupType;
    private String partitionId;
    private String appId;
    @Builder.Default
    private Boolean roleRequired = Boolean.FALSE;
}
