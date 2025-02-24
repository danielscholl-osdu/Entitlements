package org.opengroup.osdu.entitlements.v2.model.creategroup;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;

@Data
@Generated
@Builder
public class CreateGroupRepoDto {
    private EntityNode requesterNode;
    private EntityNode dataRootGroupNode;
    private boolean addDataRootGroup;
    private String partitionId;
}
