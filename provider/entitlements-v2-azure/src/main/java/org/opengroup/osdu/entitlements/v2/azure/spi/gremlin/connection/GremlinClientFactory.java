package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class GremlinClientFactory extends AbstractFactoryBean<Client> {

    @Autowired
    private Cluster cluster;

    @Override
    public Class<?> getObjectType() {
        return Client.class;
    }

    @Override
    protected Client createInstance() throws Exception {
        Client client = this.cluster.connect().alias("g");
        client.init();
        return client;
    }
}