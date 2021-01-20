package org.opengroup.osdu.entitlements.v2.model.addmember;

import org.opengroup.osdu.entitlements.v2.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberDto {
    private String email;
    private Role role;
}
