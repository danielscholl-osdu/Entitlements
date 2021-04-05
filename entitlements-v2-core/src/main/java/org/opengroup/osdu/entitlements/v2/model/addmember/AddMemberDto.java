package org.opengroup.osdu.entitlements.v2.model.addmember;

import org.opengroup.osdu.entitlements.v2.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberDto {
    @NotNull
    private String email;
    @NotNull
    private Role role;
}
