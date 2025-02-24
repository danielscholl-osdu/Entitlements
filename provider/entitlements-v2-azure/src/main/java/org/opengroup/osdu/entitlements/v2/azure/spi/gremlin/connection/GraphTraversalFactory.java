package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class GraphTraversalFactory extends AbstractFactoryBean<GraphTraversalSource> {

    @Autowired
    private Cluster cluster;

    @Override
    public Class<?> getObjectType() {
        return GraphTraversalSource.class;
    }

    @Override
    protected GraphTraversalSource createInstance() throws Exception {
        return AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(this.cluster));
    }
}