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
@Tag(name = "remove-member-api", description = "Remove Member API")
public class RemoveMemberApi {

    private final RemoveMemberService removeMemberService;
    private final RequestInfo requestInfo;
    private final PartitionHeaderValidationService partitionHeaderValidationService;
    private final RequestInfoUtilService requestInfoUtilService;

    @Operation(summary = "${removeMemberApi.deleteMember.summary}", description = "${removeMemberApi.deleteMember.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "remove-member-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content", content = {@Content(schema = @Schema(implementation = String.class ))}),
            @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
    })
    @DeleteMapping("/groups/{group_email}/members/{member_email}")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "','" + AppProperties.ADMIN + "','" + AppProperties.USERS + "')")
    public ResponseEntity<String> deleteMember(@Parameter(description = "Group Email") @PathVariable("group_email") String groupEmail,
                                               @Parameter(description = "Member Email") @PathVariable("member_email") String memberEmail) {
        String partitionId = requestInfo.getHeaders().getPartitionId();
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        String partitionDomain = requestInfoUtilService.getDomain(partitionId);
        ApiInputValidation.validateEmailAndBelongsToPartition(groupEmail, partitionDomain);
        RemoveMemberServiceDto removeMemberServiceDto = RemoveMemberServiceDto.builder()
                .groupEmail(groupEmail.toLowerCase()).memberEmail(memberEmail.toLowerCase())
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionId(partitionId).build();
        removeMemberService.removeMember(removeMemberServiceDto);
        return new ResponseEntity<>("", HttpStatus.NO_CONTENT);
    }
}
