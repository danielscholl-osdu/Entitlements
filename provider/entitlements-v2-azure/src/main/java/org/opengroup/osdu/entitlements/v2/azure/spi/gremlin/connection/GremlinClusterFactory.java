package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.util.ser.Serializers;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GremlinClusterFactory extends AbstractFactoryBean<Cluster> {

    private static final int MAX_CONTENT_LENGTH = 65536;
    private static final long KEEP_ALIVE_TIME = 30000;
    private static final int MAX_IN_PROCESS = 16;

    @Autowired
    private AzureAppProperties config;

    @Override
    public Class<?> getObjectType() {
        return Cluster.class;
    }

    @Override
    protected Cluster createInstance() throws Exception {
        try {
            return Cluster.build(getHost(this.config.getGraphDbEndpoint()))
                    .port(this.config.getGraphDbPort())
                    .credentials(this.config.getGraphDbUsername(), this.config.getGraphDbPassword())
                    .enableSsl(this.config.isGraphDbSslEnabled())
                    .maxSimultaneousUsagePerConnection(MAX_IN_PROCESS)
                    .maxInProcessPerConnection(MAX_IN_PROCESS)
                    .maxContentLength(MAX_CONTENT_LENGTH)
                    .serializer(Serializers.GRAPHSON_V2.toString())
                    .keepAliveInterval(KEEP_ALIVE_TIME)
                    .create();
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Invalid configuration of Gremlin cluster", e);
        }
    }

    private String getHost(String graphDbEndpoint) {
        return graphDbEndpoint.replace("https://", "")
                .replace(":443/", "")
                .replace("documents.azure.com", "gremlin.cosmos.azure.com");
    }
}