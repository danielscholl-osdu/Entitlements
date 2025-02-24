package org.opengroup.osdu.entitlements.v2.configuration;

import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@TestConfiguration
public class AppPropertiesTestConfiguration {

    @Bean
    public AppProperties appProperties() {
        return new AppProperties() {
            @Override
            public List<String> getInitialGroups() {
                return null;
            }

            @Override
            public String getGroupsOfServicePrincipal() {
                return null;
            }

            @Override
            public List<String> getGroupsOfInitialUsers() {
                return null;
            }

            @Override
            public List<String> getProtectedMembers() {
                return null;
            }
        };
    }
}
