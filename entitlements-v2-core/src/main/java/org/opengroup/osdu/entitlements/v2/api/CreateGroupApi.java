package org.opengroup.osdu.entitlements.v2.api;

import io.swagger.v3.oas.annotations.Operation;
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
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.GroupDto;
import org.opengroup.osdu.entitlements.v2.service.CreateGroupService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@Tag(name = "create-group-api", description = "Create Group API")
public class CreateGroupApi {

    private final CreateGroupService createGroupService;
    private final RequestInfoUtilService requestInfoUtilService;
    private final RequestInfo requestInfo;

    @Operation(summary = "${createGroupApi.createGroup.summary}", description = "${createGroupApi.createGroup.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "create-group-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created", content = { @Content(schema = @Schema(implementation = GroupDto.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "403", description = "User not authorized to perform the action.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "404", description = "Not Found",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
    })
    @PostMapping("/groups")
    @PreAuthorize("@authorizationFilter.hasAnyPermission('" + AppProperties.OPS + "', '" + AppProperties.ADMIN + "')")
    public ResponseEntity<GroupDto> createGroup(@Valid @RequestBody CreateGroupDto groupInfoDto) {
        String dataPartitionId = requestInfo.getHeaders().getPartitionId();
        String partitionDomain = requestInfoUtilService.getDomain(dataPartitionId);
        EntityNode inputGroupNode = CreateGroupDto.createGroupNode(groupInfoDto, partitionDomain, dataPartitionId);
        CreateGroupServiceDto createGroupServiceDto = CreateGroupServiceDto.builder()
                .requesterId(requestInfoUtilService.getUserId(requestInfo.getHeaders()))
                .partitionDomain(partitionDomain)
                .partitionId(dataPartitionId).build();
        EntityNode outputGroupNode = createGroupService.run(inputGroupNode, createGroupServiceDto);
        GroupDto output = GroupDto.createFromEntityNode(outputGroupNode);
        return new ResponseEntity<>(output, HttpStatus.CREATED);
    }
}
