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
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.GroupType;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupOnBehalfOfServiceDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupsOfPartitionDto;
import org.opengroup.osdu.entitlements.v2.service.ListGroupOnBehalfOfService;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "list-group-on-behalf-of-api", description = "List Group On Behalf Of API")
public class ListGroupOnBehalfOfApi {

    private static final String INVALID_FILTER_ERROR_MESSAGE = "Invalid filter";
    private final RequestInfo requestInfo;
    private final ListGroupOnBehalfOfService listGroupOnBehalfOfService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @Operation(summary = "${listGroupOnBehalfOfApi.listGroupsOnBehalfOf.summary}", description = "${listGroupOnBehalfOfApi.listGroupsOnBehalfOf.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "list-group-on-behalf-of-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = ListGroupResponseDto.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
    })
    @GetMapping("/members/{member_email}/groups")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "')")
    public ResponseEntity<ListGroupResponseDto> listGroupsOnBehalfOf(@Parameter(description = "Member Email") @PathVariable("member_email") String memberId,
        @Parameter(description = "Type of the Group. Allowable Values = \"NONE,DATA,USER,SERVICE\"", example = "NONE") @RequestParam(name = "type") String type,
        @Parameter(description = "App Id")  @RequestParam(name = "appid", required = false) String appId, @RequestParam(required = false, defaultValue = "false") Boolean roleRequired) {

        memberId = memberId.toLowerCase();
        String partitionId = requestInfo.getHeaders().getPartitionId();
        GroupType groupType = getTypeParameterCaseInsensitive(type);
        partitionHeaderValidationService.validateSinglePartitionProvided(partitionId);
        ListGroupOnBehalfOfServiceDto listGroupOnBehalfOfServiceDto = ListGroupOnBehalfOfServiceDto.builder()
                .memberId(memberId)
                .groupType(groupType)
                .partitionId(partitionId)
                .appId(appId)
                .roleRequired(roleRequired)
                .build();

        ListGroupResponseDto responseDto = listGroupOnBehalfOfService.getGroupsOnBehalfOfMember(listGroupOnBehalfOfServiceDto);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @Operation(summary = "${listGroupOnBehalfOfApi.listAllPartitionGroups.summary}", description = "${listGroupOnBehalfOfApi.listAllPartitionGroups.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "list-group-on-behalf-of-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = ListGroupsOfPartitionDto.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
    })
    @GetMapping("/groups/all")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "')")
    public ResponseEntity<ListGroupsOfPartitionDto> listAllPartitionGroups(
        @Parameter(description = "Type of the Group. Allowable Values = \"NONE,DATA,USER,SERVICE\"", example = "NONE")
        @RequestParam(name = "type") String type,
        @Parameter(description = "cursor") @RequestParam(name = "cursor", required = false) String cursor,
        @Parameter(description = "limit", example = "100")
        @RequestParam(name = "limit", required = false, defaultValue = "100") @Min(1) Integer limit
    ) {
        String partitionId = requestInfo.getHeaders().getPartitionId();
        ListGroupsOfPartitionDto groupsInPartition = listGroupOnBehalfOfService.getGroupsInPartition(partitionId, getTypeParameterCaseInsensitive(type), cursor, limit);
        return new ResponseEntity<>(groupsInPartition, HttpStatus.OK);
    }

    private GroupType getTypeParameterCaseInsensitive(String type) {
        if (type == null || type.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), INVALID_FILTER_ERROR_MESSAGE);
        } else {
            try {
                return GroupType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), INVALID_FILTER_ERROR_MESSAGE);
            }
        }
    }
}

