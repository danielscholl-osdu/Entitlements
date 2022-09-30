package org.opengroup.osdu.entitlements.v2.api;

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
public class InitApi {

    @Autowired
    private TenantInitService tenantInitService;

    @PostMapping("/tenant-provisioning")
    @PreAuthorize("@authorizationFilter.hasAnyPermission()")
    public ResponseEntity<InitServiceDto> initiateTenant(@RequestBody(required = false) InitServiceDto initServiceDto) {
        tenantInitService.createDefaultGroups();
        tenantInitService.bootstrapInitialAccounts(initServiceDto);
        return new ResponseEntity<>(initServiceDto, HttpStatus.OK);
    }
}
