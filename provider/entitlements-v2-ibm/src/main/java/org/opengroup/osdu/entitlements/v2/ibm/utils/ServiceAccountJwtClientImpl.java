package org.opengroup.osdu.entitlements.v2.ibm.utils;

import jakarta.inject.Inject;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IJwtCache;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class ServiceAccountJwtClientImpl implements IServiceAccountJwtClient {

    @Inject
    private ITenantFactory tenantInfoServiceProvider;

    @Inject
    private DpsHeaders dpsHeaders;

    @Inject
    private IJwtCache cacheService;

    @Inject
    private JaxRsDpsLog logger;

    @Inject
    private KeyCloakProvider keyCloack;

    @Value("${ibm.keycloak.useremail}")
    private String userEmail;

    @Value("${ibm.keycloak.username}")
    private String userName;

    @Value("${ibm.keycloak.password}")
    private String userPassword;

    @Override
    public String getIdToken(String tenantName) {
        /*this.log.info("Tenant name received for auth token is: " + tenantName);
        TenantInfo tenant = this.tenantInfoServiceProvider.getTenantInfo(tenantName);
        if (tenant == null) {
            this.log.error("Invalid tenant name receiving from azure");
            throw new AppException(HttpStatus.SC_BAD_REQUEST, "Invalid tenant Name", "Invalid tenant Name from azure");
        }*/
        String ACCESS_TOKEN = "";
        try {

            this.dpsHeaders.put(DpsHeaders.USER_EMAIL, userEmail);

            ACCESS_TOKEN = keyCloack.getToken(userName, userPassword);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error generating token", e);
            throw new AppException(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Persistence error", "Error generating token", e);
        }

        return ACCESS_TOKEN;
    }

}

