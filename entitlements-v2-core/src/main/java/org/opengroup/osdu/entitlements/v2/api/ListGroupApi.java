package org.opengroup.osdu.entitlements.v2.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.service.ListGroupService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.opengroup.osdu.entitlements.v2.validation.PartitionHeaderValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "list-group-api", description = "List Group API")
public class ListGroupApi {

    private final JaxRsDpsLog log;
    private final RequestInfo requestInfo;
    private final ListGroupService listGroupService;
    private final RequestInfoUtilService requestInfoUtilService;
    private final PartitionHeaderValidationService partitionHeaderValidationService;

    @Operation(summary = "${listGroupApi.listGroups.summary}", description = "${listGroupApi.listGroups.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "list-group-api" })
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
    @GetMapping("/groups")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "', '" + AppProperties.USERS + "')")
    public ResponseEntity<ListGroupResponseDto> listGroups(@RequestParam(required = false, defaultValue = "false") Boolean roleRequired) {
        DpsHeaders dpsHeaders = requestInfo.getHeaders();
        List<String> partitionIdList = requestInfoUtilService.getPartitionIdList(dpsHeaders);
        partitionHeaderValidationService.validateIfSpecialListGroupPartitionIsProvided(partitionIdList);
        String userId = requestInfoUtilService.getUserId(dpsHeaders);
        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
                .requesterId(userId)
                .appId(requestInfoUtilService.getAppId(dpsHeaders))
                .partitionIds(partitionIdList)
                .roleRequired(roleRequired)
                .build();
        ListGroupResponseDto body = ListGroupResponseDto.builder()
                .groups(new ArrayList<>(listGroupService.getGroups(listGroupServiceDto)))
                .desId(userId)
                .memberEmail(userId)
                .build();
        log.debug(String.format("ListGroupResponseDto#create done timestamp: %d", System.currentTimeMillis()));
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @Operation(summary = "${listGroupApi.listGroups.summary}", description = "${listGroupApi.listGroups.description}",
        security = {@SecurityRequirement(name = "Authorization")}, tags = { "list-group-api" })
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
    @PreAuthorize(
        "@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "', '" + AppProperties.USERS + "') "
            + "and @authorizationFilter.requesterHasImpersonationPermission('" + AppProperties.IMPERSONATOR + "') "
            + "and @authorizationFilter.targetCanBeImpersonated('" + AppProperties.IMPERSONATED_USER + "')")
    @GetMapping(value = "/groups", headers = DpsHeaders.ON_BEHALF_OF)
    public ResponseEntity<ListGroupResponseDto> listGroupsOnBehalf(){
        DpsHeaders dpsHeaders = requestInfo.getHeaders();
        List<String> partitionIdList = requestInfoUtilService.getPartitionIdList(dpsHeaders);
        partitionHeaderValidationService.validateIfSpecialListGroupPartitionIsProvided(partitionIdList);
        String impersonationTarget = requestInfoUtilService.getImpersonationTarget(dpsHeaders);

        ListGroupServiceDto listGroupServiceDto = ListGroupServiceDto.builder()
            .requesterId(impersonationTarget)
            .appId(requestInfoUtilService.getAppId(dpsHeaders))
            .partitionIds(partitionIdList)
            .build();

        ListGroupResponseDto body = ListGroupResponseDto.builder()
            .groups(new ArrayList<>(listGroupService.getGroups(listGroupServiceDto)))
            .desId(impersonationTarget)
            .memberEmail(impersonationTarget)
            .build();
        log.debug(String.format("ListGroupResponseDto#create done timestamp: %d", System.currentTimeMillis()));
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

}
