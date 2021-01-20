package org.opengroup.osdu.entitlements.v2.azure;

import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AzureAppProperties extends AppProperties {
    @Value("${app.gremlin.endpoint}")
    private String gremlinEndpoint;
    @Value("${app.gremlin.port}")
    private int gremlinPort;
    @Value("${app.gremlin.username}")
    private String gremlinUsername;
    @Value("${app.gremlin.password}")
    private String gremlinPassword;
    @Value("${app.gremlin.sslEnabled}")
    private boolean gremlinSslEnabled;
    @Value("${tenantInfo.container.name}")
    private String tenantInfoContainerName;
    @Value("${azure.cosmosdb.database}")
    private String cosmosDbName;
    @Value("${app.cosmosdb.subscriptionId}")
    private String subscriptionId;
    @Value("${app.cosmosdb.resourceGroup}")
    private String resourceGroup;
    @Value("${app.cosmosdb.cosmosDbAccountName}")
    private String cosmosDbAccountName;

    public boolean hasCosmosDbConfig() {
        return !(subscriptionId.isEmpty() && resourceGroup.isEmpty() && cosmosDbAccountName.isEmpty());
    }

    public String getGremlinEndpoint() {
        return gremlinEndpoint;
    }

    public int getGremlinPort() {
        return gremlinPort;
    }

    public String getGremlinUsername() {
        return gremlinUsername;
    }

    public String getGremlinPassword() {
        return gremlinPassword;
    }

    public boolean isGremlinSslEnabled() {
        return gremlinSslEnabled;
    }

    public String getTenantInfoContainerName() {
        return tenantInfoContainerName;
    }

    public String getCosmosDbName() {
        return cosmosDbName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getCosmosDbAccountName() {
        return cosmosDbAccountName;
    }
}
