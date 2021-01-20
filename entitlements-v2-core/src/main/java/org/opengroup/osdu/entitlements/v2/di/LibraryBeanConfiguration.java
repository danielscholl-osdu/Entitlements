package org.opengroup.osdu.entitlements.v2.di;

import org.opengroup.osdu.core.common.http.HttpClient;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.inject.Named;

@Configuration
@ComponentScan("org.opengroup.osdu.core.common")
public class LibraryBeanConfiguration {

    @Value("${azure.keyvault.url}")
    private String keyVaultURL;

    @Bean
    public IHttpClient getHttpClient() {
        return new HttpClient();
    }

    @Bean
    @Named("KEY_VAULT_URL")
    public String keyVaultURL() {
        return keyVaultURL;
    }
}
