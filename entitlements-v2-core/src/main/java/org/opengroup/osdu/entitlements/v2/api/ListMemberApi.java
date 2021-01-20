package org.opengroup.osdu.entitlements.v2.api;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberRequestArgs;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.service.ListMemberService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.opengroup.osdu.entitlements.v2.validation.ApiInputValidation;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ListMemberApi {

    private final RequestInfo requestInfo;
    private final ListMemberService listMemberService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;
    private final RequestInfoUtilService requestInfoUtilService;

    @GetMapping("/groups/{group_email}/members")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "', '" + AppProperties.USERS + "')")
    public ResponseEntity<ListMemberResponseDto> listGroupMembers(@PathVariable("group_email") String groupEmail,
                                                                  @RequestParam(value = "role", required = false) Role role,
                                                                  @RequestParam(value = "includeType", required = false) boolean includeType) {

        String partitionId = requestInfo.getHeaders().getPartitionId();
        String partitionDomain = requestInfoUtilService.getDomain(partitionId);
        performValidation(groupEmail, partitionId, partitionDomain);
        ListMemberServiceDto listMemberServiceDto = ListMemberServiceDto.builder()
                .groupId(groupEmail.toLowerCase())
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionId(partitionId).build();
        List<ChildrenReference> members = listMemberService.run(listMemberServiceDto);
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().role(role).includeType(includeType).build();
        ListMemberResponseDto listMemberResponseDto = ListMemberResponseDto.create(members, args);
        return new ResponseEntity<>(listMemberResponseDto, HttpStatus.OK);
    }

    private void performValidation(String groupEmail, String partitionId, String partitionDomain) {
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        ApiInputValidation.validateEmailAndBelongsToPartition(groupEmail, partitionDomain);
    }
}
