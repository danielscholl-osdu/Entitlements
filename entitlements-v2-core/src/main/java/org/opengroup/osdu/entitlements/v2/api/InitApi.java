package org.opengroup.osdu.entitlements.v2.api;

import org.opengroup.osdu.entitlements.v2.service.TenantInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InitApi {

    @Autowired
    private TenantInitService tenantInitService;

    @PostMapping("/tenant-provisioning")
    @PreAuthorize("@authorizationFilter.hasAnyPermission()")
    public ResponseEntity<Void> initiateTenant() {
        tenantInitService.createDefaultGroups();
        tenantInitService.bootstrapInitialAccounts();
        return ResponseEntity.ok().build();
    }
}
