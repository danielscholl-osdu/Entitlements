package org.opengroup.osdu.entitlements.v2.azure.configuration;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.azure.di.RedisAzureConfiguration;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.EmbeddedGremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class AzureTestConfiguration {

    @Primary
    @Bean
    public RedisAzureCache<String, ParentReferences> groupCache() {
        return new RedisAzureCache<>(String.class, ParentReferences.class,
                new RedisAzureConfiguration(0, 3600, 7000, 3600, 5));
    }

    @Primary
    @Bean
    public GremlinConnector gremlinConnector() {
        return new EmbeddedGremlinConnector();
    }

    @Primary
    @Bean
    public SecretClient getSecretClient() {
        SecretClient secret = mock(SecretClient.class);
        when(secret.getSecret("app-dev-sp-username")).thenReturn(new KeyVaultSecret("key", "value"));
        when(secret.getSecret("app-dev-sp-password")).thenReturn(new KeyVaultSecret("key", "value"));
        when(secret.getSecret("app-dev-sp-tenant-id")).thenReturn(new KeyVaultSecret("key", "value"));

        return secret;
    }
}