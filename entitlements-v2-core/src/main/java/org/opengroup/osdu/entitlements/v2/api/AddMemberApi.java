package org.opengroup.osdu.entitlements.v2.api;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.service.AddMemberService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.opengroup.osdu.entitlements.v2.validation.ApiInputValidation;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class AddMemberApi {

    private final AddMemberService addMemberService;
    private final RequestInfo requestInfo;
    private final AppProperties config;
    private final RequestInfoUtilService requestInfoUtilService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @PostMapping("/groups/{group_email}/members")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "','" + AppProperties.ADMIN + "','" + AppProperties.USERS + "')")
    public ResponseEntity<AddMemberDto> addMember(@Valid @RequestBody AddMemberDto addMemberDto,
                                                  @PathVariable("group_email") String groupEmail) {
        addMemberDto.setEmail(addMemberDto.getEmail().toLowerCase());
        String partitionIdHeader = requestInfo.getHeaders().getPartitionId();
        String partitionDomain = requestInfoUtilService.getDomain(partitionIdHeader);
        performValidation(groupEmail, addMemberDto, partitionIdHeader, partitionDomain);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail(groupEmail.toLowerCase())
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionId(partitionIdHeader)
                .build();
        addMemberService.run(addMemberDto, addMemberServiceDto);
        return new ResponseEntity<>(addMemberDto, HttpStatus.OK);
    }

    private void performValidation(String groupEmail, AddMemberDto addMemberDto, String partitionId, String partitionDomain) {
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        ApiInputValidation.validateEmailAndBelongsToPartition(groupEmail, partitionDomain);
        ApiInputValidation.validateEmailAgainstCrossPartition(addMemberDto.getEmail(), config.getDomain(), partitionDomain);
    }
}
