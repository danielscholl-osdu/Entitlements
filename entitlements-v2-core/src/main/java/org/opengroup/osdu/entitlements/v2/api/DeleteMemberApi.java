package org.opengroup.osdu.entitlements.v2.api;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.deletemember.DeleteMemberDto;
import org.opengroup.osdu.entitlements.v2.service.DeleteMemberService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeleteMemberApi {
    private final RequestInfo requestInfo;
    private final DeleteMemberService deleteMemberService;
    private final RequestInfoUtilService requestInfoUtilService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @DeleteMapping("/members/{member_email}")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "')")
    public ResponseEntity<Void> deleteMember(@PathVariable("member_email") String memberEmail) {
        String partitionId = requestInfo.getHeaders().getPartitionId();
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        deleteMemberService.deleteMember(buildDeleteMemberDto(memberEmail, partitionId));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private DeleteMemberDto buildDeleteMemberDto(String memberEmail, String partitionId) {
        return DeleteMemberDto.builder()
                .memberEmail(memberEmail.toLowerCase())
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionId(partitionId)
                .build();
    }
}
