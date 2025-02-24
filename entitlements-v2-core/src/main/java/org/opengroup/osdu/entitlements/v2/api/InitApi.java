package org.opengroup.osdu.entitlements.v2.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.opengroup.osdu.core.common.model.http.AppError;
import org.opengroup.osdu.entitlements.v2.model.init.InitServiceDto;
import org.opengroup.osdu.entitlements.v2.service.TenantInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "init-api", description = "Init API")
public class InitApi {

    @Autowired
    private TenantInitService tenantInitService;

    @Operation(summary = "${initApi.initiateTenant.summary}", description = "${initApi.initiateTenant.description}",
            security = {@SecurityRequirement(name = "Authorization")}, tags = { "init-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = InitServiceDto.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad Request",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "401", description = "Unauthorized",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "404", description = "Not found.",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "500", description = "Internal Server Error",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "502", description = "Bad Gateway",  content = {@Content(schema = @Schema(implementation = AppError.class ))}),
            @ApiResponse(responseCode = "503", description = "Service Unavailable",  content = {@Content(schema = @Schema(implementation = AppError.class ))})
    })
    @PostMapping("/tenant-provisioning")
    @PreAuthorize("@authorizationFilter.hasAnyPermission()")
    public ResponseEntity<InitServiceDto> initiateTenant(@RequestBody(required = false) InitServiceDto initServiceDto) {
        tenantInitService.createDefaultGroups();
        tenantInitService.bootstrapInitialAccounts(initServiceDto);
        return new ResponseEntity<>(initServiceDto, HttpStatus.OK);
    }
}
