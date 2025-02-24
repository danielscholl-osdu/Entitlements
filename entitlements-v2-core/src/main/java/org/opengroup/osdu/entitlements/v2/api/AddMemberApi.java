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

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@Tag(name = "add-member-api", description = "Add Member API")
public class AddMemberApi {
    private final AddMemberService addMemberService;
    private final RequestInfo requestInfo;
    private final RequestInfoUtilService requestInfoUtilService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @Operation(summary = "${addMemberApi.addMember.summary}", description = "${addMemberApi.addMember.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "add-member-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = AddMemberDto.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
    })
    @PostMapping("/groups/{group_email}/members")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "','" + AppProperties.ADMIN + "','" + AppProperties.USERS + "')")
    public ResponseEntity<AddMemberDto> addMember(@Valid @RequestBody AddMemberDto addMemberDto,
                                                  @Parameter(description = "Group Email")
                                                  @PathVariable("group_email") String groupEmail) {
        addMemberDto.setEmail(addMemberDto.getEmail().toLowerCase());
        String partitionId = requestInfo.getHeaders().getPartitionId();
        performValidation(groupEmail, partitionId);
        AddMemberServiceDto addMemberServiceDto = AddMemberServiceDto.builder()
                .groupEmail(groupEmail.toLowerCase())
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionId(partitionId)
                .build();
        addMemberService.run(addMemberDto, addMemberServiceDto);
        return new ResponseEntity<>(addMemberDto, HttpStatus.OK);
    }

    private void performValidation(String groupEmail, String partitionId) {
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        String partitionDomain = requestInfoUtilService.getDomain(partitionId);
        ApiInputValidation.validateEmailAndBelongsToPartition(groupEmail, partitionDomain);
    }
}
