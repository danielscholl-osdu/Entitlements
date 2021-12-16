package org.opengroup.osdu.entitlements.v2.jdbc.security;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;

@RequiredArgsConstructor
public class ExternalAuthorizationFilter {

    private final AuthorizationService authService;
    private final RequestInfo requestInfo;
    private final JaxRsDpsLog log;
    private final RequestInfoUtilService requestInfoUtilService;

    public boolean hasAnyPermission(String... requiredRoles) {
        log.debug(String.format("authorizeAny timestamp: %d", System.currentTimeMillis()));
        DpsHeaders headers = requestInfo.getHeaders();
        String user = requestInfoUtilService.getUserId(headers);
        if (user == null) {
            throw AppException.createUnauthorized("No User Id header provided");
        }
        requestInfo.getHeaders().put(DpsHeaders.USER_EMAIL, user);
        TenantInfo tenantInfo = requestInfo.getTenantInfo();
        if (tenantInfo == null) {
            throw AppException.createForbidden("Invalid data partition id");
        }
        if (user.equalsIgnoreCase(tenantInfo.getServiceAccount())) {
            return true;
        }
        if (requiredRoles.length <= 0) {
            throw AppException.createUnauthorized("The user is not authorized to perform this action");
        }
        return authService.isAuthorized(headers, requiredRoles);
    }
}
