package org.opengroup.osdu.entitlements.v2.model.creategroup;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Generated
@Builder
public class CreateGroupServiceDto {
    private String requesterId;
    private String partitionDomain;
    private String partitionId;
}
