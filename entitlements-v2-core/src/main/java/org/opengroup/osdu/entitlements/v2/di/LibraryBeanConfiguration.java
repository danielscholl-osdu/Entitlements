package org.opengroup.osdu.entitlements.v2.di;

import org.opengroup.osdu.core.common.http.HttpClient;
import org.opengroup.osdu.core.common.http.IHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.opengroup.osdu.core.common")
public class LibraryBeanConfiguration {

    @Bean
    public IHttpClient getHttpClient() {
        return new HttpClient();
    }
}
