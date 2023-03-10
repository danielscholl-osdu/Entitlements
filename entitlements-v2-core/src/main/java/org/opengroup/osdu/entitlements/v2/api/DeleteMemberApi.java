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
@Tag(name = "delete-member-api", description = "Delete Member API")
public class DeleteMemberApi {
    private final RequestInfo requestInfo;
    private final DeleteMemberService deleteMemberService;
    private final RequestInfoUtilService requestInfoUtilService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @Operation(summary = "${deleteMemberApi.deleteMember.summary}", description = "${deleteMemberApi.deleteMember.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "delete-member-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content"),
            @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
    })
    @DeleteMapping("/members/{member_email}")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "')")
    public ResponseEntity<Void> deleteMember(@Parameter(description = "Member Email") @PathVariable("member_email") String memberEmail) {
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
