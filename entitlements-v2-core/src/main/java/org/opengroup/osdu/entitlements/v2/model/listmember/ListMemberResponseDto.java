package org.opengroup.osdu.entitlements.v2.model.listmember;

import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@Generated
@AllArgsConstructor
@NoArgsConstructor
public class ListMemberResponseDto {
    List<MemberDto> members;

    public static ListMemberResponseDto create(List<ChildrenReference> members, ListMemberRequestArgs args) {
        List<MemberDto> memberDtos = new ArrayList<>();
        Boolean includeType = args.getIncludeType();
        Role role = args.getRole();

        if (role == null) {
            members.stream().forEach(member -> memberDtos.add(MemberDto.create(member, includeType)));
        } else {
            members.stream()
                    .filter(member -> member.getRole().equals(role))
                    .collect(Collectors.toList())
                    .forEach(member -> memberDtos.add(MemberDto.create(member, includeType)));
        }

        return ListMemberResponseDto.builder()
                .members(memberDtos)
                .build();
    }
}
