package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.model.deletemember.DeleteMemberDto;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.validation.BootstrapGroupsConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeleteMemberService {
    private final JaxRsDpsLog log;
    private final GroupCacheService groupCacheService;
    private final RemoveMemberService removeMemberService;
    private final BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;

    public void deleteMember(DeleteMemberDto deleteMemberDto) {
        log.info(String.format("Remove member %s from all groups", deleteMemberDto.getMemberEmail()));

        String elementaryUsersGroupForPartition = bootstrapGroupsConfigurationService.getElementaryDataPartitionUsersGroup(deleteMemberDto.getPartitionId());
        Set<String> directParents = removeMemberService.getDirectParentsEmails(deleteMemberDto.getPartitionId(), deleteMemberDto.getMemberEmail());

        //we need to remove the user from elementary data partition users group in the end
        directParents.stream()
                .filter(groupName -> !groupName.equals(elementaryUsersGroupForPartition))
                .map(groupEmail -> buildRemoveMemberServiceDto(groupEmail, deleteMemberDto))
                .forEach(this::removeMemberFromGroup);

        if (directParents.contains(elementaryUsersGroupForPartition))
            removeMemberFromGroup(buildRemoveMemberServiceDto(elementaryUsersGroupForPartition, deleteMemberDto));

        groupCacheService.flushListGroupCacheForUser(deleteMemberDto.getMemberEmail(), deleteMemberDto.getPartitionId());
    }


    private RemoveMemberServiceDto buildRemoveMemberServiceDto(String groupEmail, DeleteMemberDto deleteMemberDto) {
        return RemoveMemberServiceDto.builder()
                .groupEmail(groupEmail)
                .memberEmail(deleteMemberDto.getMemberEmail())
                .requesterId(deleteMemberDto.getRequesterId())
                .partitionId(deleteMemberDto.getPartitionId())
                .build();
    }

    private void removeMemberFromGroup(RemoveMemberServiceDto removeMemberServiceDto) {
        try {
            removeMemberService.removeMember(removeMemberServiceDto);
        } catch (AppException e) {
            log.warning("Not handling AppException");
            throw e;
        } catch (Exception e) {
            log.error(String.format("Error when removing member: %s from group: %s, reason: %s",
                    removeMemberServiceDto.getMemberEmail(), removeMemberServiceDto.getGroupEmail(), e.getMessage()));
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Error while removing member", e);
        }
    }
}
