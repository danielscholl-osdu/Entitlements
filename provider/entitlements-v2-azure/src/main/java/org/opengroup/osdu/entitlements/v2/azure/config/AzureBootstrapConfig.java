package org.opengroup.osdu.entitlements.v2.azure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBootstrapConfig {

    @Bean(name="KEY_VAULT_URL")
    public String keyVaultURL(@Value("${azure.keyvault.url}") String keyVaultURL) {
        return keyVaultURL;
    }
}
