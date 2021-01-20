package org.opengroup.osdu.entitlements.v2.configuration;

import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class AppPropertiesTestConfiguration {

    @Bean
    public AppProperties appProperties() {
        return new AppProperties() {
        };
    }
}
