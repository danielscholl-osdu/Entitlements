package org.opengroup.osdu.entitlements.v2.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/_ah")
public class HealthChecksApi {

    @GetMapping("/liveness_check")
    public ResponseEntity<Void> livenessCheck() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/readiness_check")
    public ResponseEntity<Void> readinessCheck() {
        return ResponseEntity.ok().build();
    }
}
