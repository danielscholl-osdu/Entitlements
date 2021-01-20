package org.opengroup.osdu.entitlements.v2.api;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.service.RemoveMemberService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.opengroup.osdu.entitlements.v2.validation.ApiInputValidation;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RemoveMemberApi {

    private final RemoveMemberService removeMemberService;
    private final RequestInfo requestInfo;
    private final PartitionHeaderValidationService partitionHeaderValidationService;
    private final RequestInfoUtilService requestInfoUtilService;

    @DeleteMapping("/groups/{group_email}/members/{member_email}")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "','" + AppProperties.ADMIN + "','" + AppProperties.USERS + "')")
    public ResponseEntity<String> deleteMember(@PathVariable("group_email") String groupEmail,
                                               @PathVariable("member_email") String memberEmail) {
        String partitionId = requestInfo.getHeaders().getPartitionId();
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        String partitionDomain = requestInfoUtilService.getDomain(partitionId);
        ApiInputValidation.validateEmailAndBelongsToPartition(groupEmail, partitionDomain);
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail(groupEmail.toLowerCase()).memberEmail(memberEmail.toLowerCase())
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionId(partitionId).build();
        removeMemberService.removeMember(removeMemberServiceDto);
        return new ResponseEntity<>("", HttpStatus.ACCEPTED);
    }
}
