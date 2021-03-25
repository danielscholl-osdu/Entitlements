package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupOnBehalfOfServiceDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupServiceDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListGroupOnBehalfOfService {

    private final ListGroupService listGroupService;
    private final JaxRsDpsLog log;

    public ListGroupResponseDto getGroupsOnBehalfOfMember(ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto) {
        String memberId = listGroupOnBehalfOfServiceDto.getMemberId();
        log.info(String.format("requesting groups for %s", memberId));

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId(memberId)
                .appId(listGroupOnBehalfOfServiceDto.getAppId())
                .partitionIds(Collections.singletonList(listGroupOnBehalfOfServiceDto.getPartitionId())).build();

        Set<ParentReference> groups = listGroupService.getGroups(listGroupServiceDto);
        ListGroupResponseDto listGroupResponse = filterGroups(groups, listGroupOnBehalfOfServiceDto.getGroupType(), memberId);

        return listGroupResponse;
    }

    private ListGroupResponseDto filterGroups(Set<ParentReference> parents, GroupType groupType, String memberId) {
        ListGroupResponseDto output = new ListGroupResponseDto();
        if (!GroupType.NONE.equals(groupType)) {
            output.setGroups(parents.stream().filter(parent -> parent.isMatchGroupType(groupType)).collect(Collectors.toList()));
        } else {
            output.setGroups(new ArrayList<>(parents));
        }
        output.setDesId(memberId);
        output.setMemberEmail(memberId);
        return output;
    }
}
