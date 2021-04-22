package org.opengroup.osdu.entitlements.v2.api;

import org.opengroup.osdu.entitlements.v2.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/_ah")
public class HealthChecksApi {

    @Autowired
    private HealthService healthService;

    @GetMapping("/liveness_check")
    public ResponseEntity<Void> livenessCheck() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/readiness_check")
    public ResponseEntity<Void> readinessCheck() {
        healthService.performHealthCheck();
        return ResponseEntity.ok().build();
    }
}
