package org.opengroup.osdu.entitlements.v2.azure.configuration;

import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.EmbeddedGremlinConnector;
import org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection.GremlinConnector;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class GremlinTestConfiguration {

    @Bean
    public GremlinConnector gremlinConnector() {
        return new EmbeddedGremlinConnector();
    }
}
