package org.opengroup.osdu.entitlements.v2.gcp.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.auth.TokenProvider;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceAccountJwtGcpClientImpl implements IServiceAccountJwtClient {

    private final TokenProvider tokenProvider;

    @Override
    public String getIdToken(String tenantName) {
        log.debug("Tenant name received for auth token is: {}", tenantName);
        return "Bearer " + tokenProvider.getIdToken();
    }
}
