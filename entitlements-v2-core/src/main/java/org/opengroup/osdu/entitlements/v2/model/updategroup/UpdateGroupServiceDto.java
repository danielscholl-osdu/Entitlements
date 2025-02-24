package org.opengroup.osdu.entitlements.v2.model.updategroup;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Builder
@Generated
public class UpdateGroupServiceDto {
    private String existingGroupEmail;
    private String partitionId;
    private String partitionDomain;
    private String requesterId;
    private UpdateGroupOperation renameOperation;
    private UpdateGroupOperation appIdsOperation;
}
