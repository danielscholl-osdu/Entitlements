package org.opengroup.osdu.entitlements.v2.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.service.MembersCountService;
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

@RestController
@RequiredArgsConstructor
@Tag(name = "members-count-api", description = "Count Members of a group")
public class MembersCountApi {
    private final RequestInfo requestInfo;
    private final PartitionHeaderValidationService partitionHeaderValidationService;
    private final RequestInfoUtilService requestInfoUtilService;
    private final MembersCountService membersCountService;

    @Operation(summary = "${membersCountApi.getMemberCount.summary}", description = "${membersCountApi.getMemberCount.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = {"members-count-api"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = {@Content(schema = @Schema(implementation = ListMemberResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.", content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "404", description = "Not Found", content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway", content = {@Content(schema = @Schema(implementation = AppError.class))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable", content = {@Content(schema = @Schema(implementation = AppError.class))})
    })
    @GetMapping("/groups/{group_email}/membersCount")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "', '" + AppProperties.USERS + "')")
    public ResponseEntity<MembersCountResponseDto> getMembersCount(@Parameter(description = "Group Email") @PathVariable("group_email") String groupEmail, @RequestParam(value = "role", required = false) Role role) {
        //generic validation for data partition and user belonging to the group_email provided
        String partitionId = requestInfo.getHeaders().getPartitionId();
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        String partitionDomain = requestInfoUtilService.getDomain(partitionId);
        ApiInputValidation.validateEmailAndBelongsToPartition(groupEmail, partitionDomain);

        MembersCountServiceDto membersCountServiceDto = MembersCountServiceDto.builder()
                .groupId(groupEmail.toLowerCase())
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionId(partitionId)
                .role(role)
                .build();

        return new ResponseEntity<>(membersCountService.getMembersCount(membersCountServiceDto), HttpStatus.OK);
    }
}
