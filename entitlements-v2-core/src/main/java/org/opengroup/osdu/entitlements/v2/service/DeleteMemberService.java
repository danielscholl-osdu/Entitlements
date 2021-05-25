package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.deletemember.DeleteMemberDto;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeleteMemberService {
    private final JaxRsDpsLog log;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCacheService groupCacheService;
    private final RemoveMemberService removeMemberService;

    public void deleteMember(DeleteMemberDto deleteMemberDto) {
        log.info(String.format("Remove member %s from all groups", deleteMemberDto.getMemberEmail()));
        createRemoveMemberServiceDtos(deleteMemberDto).forEach(this::removeMemberFromGroup);
        groupCacheService.flushListGroupCacheForUser(deleteMemberDto.getMemberEmail(), deleteMemberDto.getPartitionId());
    }

    private List<RemoveMemberServiceDto> createRemoveMemberServiceDtos(DeleteMemberDto deleteMemberDto) {
        return loadDirectParentsEmails(deleteMemberDto).stream()
                .map(groupEmail -> buildRemoveMemberServiceDto(groupEmail, deleteMemberDto))
                .collect(Collectors.toList());
    }

    private RemoveMemberServiceDto buildRemoveMemberServiceDto(String groupEmail, DeleteMemberDto deleteMemberDto) {
        return RemoveMemberServiceDto.builder()
                .groupEmail(groupEmail)
                .memberEmail(deleteMemberDto.getMemberEmail())
                .requesterId(deleteMemberDto.getRequesterId())
                .partitionId(deleteMemberDto.getPartitionId())
                .build();
    }

    private Set<String> loadDirectParentsEmails(DeleteMemberDto deleteMemberDto) {
        List<ParentReference> directParents = retrieveGroupRepo.loadDirectParents(
                deleteMemberDto.getPartitionId(), deleteMemberDto.getMemberEmail());
        return directParents.stream()
                .map(ParentReference::getId)
                .collect(Collectors.toSet());
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
