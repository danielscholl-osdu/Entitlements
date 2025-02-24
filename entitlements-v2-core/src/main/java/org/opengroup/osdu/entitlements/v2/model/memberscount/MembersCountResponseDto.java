package org.opengroup.osdu.entitlements.v2.model.memberscount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Generated
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Represents a model for the counting the members of a group")
public class MembersCountResponseDto {
    @Schema(description = "Group Email")
    String groupEmail;

    @Schema(description = "Members count present in the group")
    @Builder.Default
    int membersCount = 0;
}
