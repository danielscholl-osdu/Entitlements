package org.opengroup.osdu.entitlements.v2.model.addmember;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opengroup.osdu.entitlements.v2.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a model to add a member")
public class AddMemberDto {

    @Schema(description = "Email Id of the member")
    @NotNull
    private String email;

    @Schema(description = "Role of the member")
    @NotNull
    private Role role;
}
