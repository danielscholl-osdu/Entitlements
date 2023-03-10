package org.opengroup.osdu.entitlements.v2.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.opengroup.osdu.entitlements.v2.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/_ah")
@Tag(name = "health-checks-api", description = "Health Checks API")
public class HealthChecksApi {

    @Autowired
    private HealthService healthService;

    @Operation(summary = "${healthChecksApi.livenessCheck.summary}",
            description = "${healthChecksApi.livenessCheck.description}", tags = { "health-checks-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = String.class)) })
    })
    @GetMapping("/liveness_check")
    public ResponseEntity<Void> livenessCheck() {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "${healthChecksApi.readinessCheck.summary}",
            description = "${healthChecksApi.readinessCheck.description}", tags = { "health-checks-api" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = { @Content(schema = @Schema(implementation = String.class)) })
    })
    @GetMapping("/readiness_check")
    public ResponseEntity<Void> readinessCheck() {
        healthService.performHealthCheck();
        return ResponseEntity.ok().build();
    }
}
