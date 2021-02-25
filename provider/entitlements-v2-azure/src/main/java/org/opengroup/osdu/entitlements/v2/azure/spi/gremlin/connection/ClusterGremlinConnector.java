package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.exception.ResponseException;
import org.apache.tinkerpop.gremlin.driver.message.ResponseStatusCode;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.translator.GroovyTranslator;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeEdge;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.service.VertexUtilService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class ClusterGremlinConnector implements GremlinConnector {
    private static final int MAX_CONTENT_LENGTH = 65536;
    private static final long KEEP_ALIVE_TIME = 30000;
    private static final int MAX_IN_PROCESS = 16;
    private static final String NOT_FOUND_EXCEPTION_TYPE = "NotFoundException";
    private static final String TRAVERSAL_SUBMIT_ERROR_MESSAGE = "Error submitting traversal";
    private static final String RETRIEVING_RESULT_SET_ERROR_MESSAGE = "Error retrieving ResultSet object";
    private static final String RESOURCE_NOT_FOUND_ERROR_MESSAGE = "Resource Not Found";
    private static final String HTTPS_SCHEME = "https://";
    private static final String ENDPOINT_PORT = ":443/";
    /**
     * .NET SDK URI comes in form https://xxx.documents.azure.com:443/
     */
    private static final String NET_HOST_POSTFIX = "documents.azure.com";
    /**
     * Gremlin Endpoint comes in form wss://xxx.gremlin.cosmos.azure.com:443/
     */
    private static final String GREMLIN_HOST_POSTFIX = "gremlin.cosmos.azure.com";
    private static final String G = "g";
    private final AzureAppProperties config;
    private final VertexUtilService vertexUtilService;
    private Client client;
    private GraphTraversalSource graphTraversalSource;

    @PostConstruct
    protected void init() {
        /* TODO: Need to improve connection pool as it currently only creates connection & open connection pool
            during init. This method does not support large concurrent calls. US #618673
        */
        Cluster cluster = buildCluster();
        client = cluster.connect().alias(G);
        graphTraversalSource = AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster));
    }

    @Override
    public GraphTraversalSource getGraphTraversalSource() {
         return graphTraversalSource;
    }

    @Override
    public void addEdge(Traversal<Vertex, Edge> traversal) {
        submitTraversalAsQueryString(traversal);
    }

    @Override
    public void removeEdge(Traversal<Vertex, Edge> traversal) {
        submitTraversalAsQueryString(traversal);
    }

    @Override
    public NodeVertex addVertex(Traversal<Vertex, Vertex> traversal) {
        return vertexUtilService.getVertexFromResultList(submitTraversalAsQueryString(traversal));
    }

    @Override
    public boolean hasVertex(Traversal<Vertex, Vertex> traversal) {
        return getVertex(traversal).isPresent();
    }

    @Override
    public List<NodeVertex> getVertices(Traversal<Vertex, Vertex> traversal) {
        return vertexUtilService.getVerticesFromResultList(submitTraversalAsQueryString(traversal));
    }

    @Override
    public Optional<NodeVertex> getVertex(Traversal<Vertex, Vertex> traversal) {
        return Optional.ofNullable(vertexUtilService.getVertexFromResultList(submitTraversalAsQueryString(traversal)));
    }

    @Override
    public void removeVertex(Traversal<Vertex, Vertex> traversal) {
        submitTraversalAsQueryString(traversal);
    }

    @Override
    public List<Map<NodeVertex, NodeEdge>> getVerticesAndEdges(Traversal<Vertex, Map<String, Object>> traversal) {
        return vertexUtilService.getVerticesAndEdgesFromResultList(submitTraversalAsQueryString(traversal));
    }

    @Override
    public void updateVertex(Traversal<Vertex, Vertex> traversal) {
        submitTraversalAsQueryString(traversal);
    }

    private List<Result> submitTraversalAsQueryString(Traversal<?, ?> traversal) {
        String query = GroovyTranslator.of(G).translate(traversal.asAdmin().getBytecode());
        return getResultList(client.submit(query));
    }

    private Cluster buildCluster() {
        try {
            return Cluster.build(getHost(config.getGraphDbEndpoint()))
                    .port(config.getGraphDbPort())
                    .credentials(config.getGraphDbUsername(), config.getGraphDbPassword())
                    .enableSsl(config.isGraphDbSslEnabled())
                    .maxSimultaneousUsagePerConnection(MAX_IN_PROCESS)
                    .maxInProcessPerConnection(MAX_IN_PROCESS)
                    .maxContentLength(MAX_CONTENT_LENGTH)
                    .serializer(Serializers.GRAPHSON_V2D0.toString())
                    .keepAliveInterval(KEEP_ALIVE_TIME)
                    .create();
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Invalid configuration of Gremlin cluster", e);
        }
    }

    /**
     *  Extracts host from endpoint,
     *  Note: keyvault has value: https://host:443/
     */
    private String getHost(String graphDbEndpoint) {
        return graphDbEndpoint.replace(HTTPS_SCHEME, "")
                .replace(ENDPOINT_PORT, "")
                .replace(NET_HOST_POSTFIX, GREMLIN_HOST_POSTFIX);
    }

    private List<Result> getResultList(ResultSet resultSet) {
        final CompletableFuture<List<Result>> completableFutureResults;
        final CompletableFuture<Map<String, Object>> completableFutureStatusAttributes;
        final List<Result> resultList;
        try {
            completableFutureStatusAttributes = resultSet.statusAttributes();
            completableFutureResults = resultSet.all();
            resultList = completableFutureResults.get();
            validateRequestSuccessful(completableFutureStatusAttributes.get());
        } catch (ExecutionException e) {
            if (isResourceNotFoundException(e)) {
                throw new AppException(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        RESOURCE_NOT_FOUND_ERROR_MESSAGE, e);
            }
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    RETRIEVING_RESULT_SET_ERROR_MESSAGE, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    RETRIEVING_RESULT_SET_ERROR_MESSAGE, e);
        }
        return resultList;
    }

    private boolean isResourceNotFoundException(ExecutionException e) {
        if (e.getCause() instanceof ResponseException) {
            ResponseException responseException = (ResponseException) e.getCause();
            if (ResponseStatusCode.SERVER_ERROR.equals(responseException.getResponseStatusCode())) {
                return StringUtils.contains(responseException.getMessage(), NOT_FOUND_EXCEPTION_TYPE);
            }
        }
        return false;
    }

    private void validateRequestSuccessful(Map<String, Object> statusAttributes) {
        if ((Integer) statusAttributes.get("x-ms-status-code") != 200) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    TRAVERSAL_SUBMIT_ERROR_MESSAGE);
        }
    }
}
