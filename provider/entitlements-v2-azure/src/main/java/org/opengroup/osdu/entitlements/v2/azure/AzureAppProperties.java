package org.opengroup.osdu.entitlements.v2.azure;

import com.azure.security.keyvault.secrets.SecretClient;
import lombok.Getter;
import org.opengroup.osdu.azure.KeyVaultFacade;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class AzureAppProperties extends AppProperties {

    @Autowired
    private SecretClient secretClient;
    @Value("${app.graph.db.port}")
    private int graphDbPort;
    @Value("${app.graph.db.username}")
    private String graphDbUsername;
    @Value("${app.graph.db.sslEnabled}")
    private boolean graphDbSslEnabled;
    @Value("${tenantInfo.container.name}")
    private String tenantInfoContainerName;
    @Value("${azure.cosmosdb.database}")
    private String cosmosDbName;

    public String getGraphDbPassword() {
        return KeyVaultFacade.getSecretWithValidation(secretClient, "graph-db-primary-key");
    }

    public String getGraphDbEndpoint() {
        return KeyVaultFacade.getSecretWithValidation(secretClient, "graph-db-endpoint");
    }

    @Override
    public List<String> getInitialGroups() {
        List<String> initialGroups = new ArrayList<>();
        initialGroups.add("/provisioning/groups/datalake_user_groups.json");
        initialGroups.add("/provisioning/groups/datalake_service_groups.json");
        initialGroups.add("/provisioning/groups/data_groups.json");
        return initialGroups;
    }

    @Override
    public String getGroupsOfServicePrincipal() {
        return "/provisioning/accounts/groups_of_service_principal.json";
    }

    @Override
    public List<String> getProtectedMembers() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/provisioning/groups/data_groups.json");
        filePaths.add("/provisioning/groups/datalake_service_groups.json");
        return filePaths;
    }
}
