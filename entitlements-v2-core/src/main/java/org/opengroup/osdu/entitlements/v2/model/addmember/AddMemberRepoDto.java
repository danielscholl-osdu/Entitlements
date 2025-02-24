package org.opengroup.osdu.entitlements.v2.model.addmember;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;

import java.util.Set;

@Data
@Generated
@Builder
public class AddMemberRepoDto {
    private EntityNode memberNode;
    private Role role;
    private String partitionId;
    private Set<ParentReference> existingParents;
}
