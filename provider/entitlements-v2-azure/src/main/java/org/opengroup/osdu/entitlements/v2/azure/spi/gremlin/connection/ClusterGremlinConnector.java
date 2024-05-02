package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.exception.ResponseException;
import org.apache.tinkerpop.gremlin.driver.message.ResponseStatusCode;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.translator.GroovyTranslator;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.azure.logging.DependencyLogger;
import org.opengroup.osdu.azure.logging.DependencyLoggingOptions;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeEdge;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.service.VertexUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class ClusterGremlinConnector implements GremlinConnector {
    private static final String NOT_FOUND_EXCEPTION_TYPE = "NotFoundException";
    private static final String TRAVERSAL_SUBMIT_ERROR_MESSAGE = "Error submitting traversal";
    private static final String RETRIEVING_RESULT_SET_ERROR_MESSAGE = "Error retrieving ResultSet object";
    private static final String COSMOS_DB_RATE_LIMIT_ERROR_MESSAGE = "Request rate to Cosmos DB is too large";
    private static final String RESOURCE_NOT_FOUND_ERROR_MESSAGE = "Resource Not Found";
    private static final String G = "g";

    @Autowired
    private VertexUtilService vertexUtilService;

    @Autowired
    private Client client;

    @Autowired
    private GraphTraversalSource graphTraversalSource;

    @Autowired
    private JaxRsDpsLog log;

    @Autowired
    private DependencyLogger dependencyLogger;

    @Override
    public GraphTraversalSource getGraphTraversalSource() {
        return this.graphTraversalSource;
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

    @Override
    public List<NodeEdge> getEdge(GraphTraversal<Vertex, List<Edge>> traversal) {
        return vertexUtilService.getNodeEdgesFromResultList(submitTraversalAsQueryString(traversal));
    }

    private List<Result> submitTraversalAsQueryString(Traversal<?, ?> traversal) {
        String query = GroovyTranslator.of(G).translate(traversal.asAdmin().getBytecode()).getScript();
        return getResultList(client.submit(query));
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

            long latency = ((Double)completableFutureStatusAttributes.get().get("x-ms-total-server-time-ms")).longValue();
            double requestCharge = (Double)completableFutureStatusAttributes.get().get("x-ms-total-request-charge");
            String requestId = resultSet.getOriginalRequestMessage().getArgs().toString();
            DependencyLoggingOptions loggingOptions = DependencyLoggingOptions.builder().type("CosmosStore").name("COSMOS_GRAPH").data(requestId).timeTakenInMs(latency).requestCharge(requestCharge).resultCode(200).success(true).build();
            this.dependencyLogger.logDependency(loggingOptions);

        } catch (ExecutionException e) {
            log.error(String.format("getting error from cosmos db: %s", e.getMessage()), e);
            if (isResourceNotFoundException(e)) {
                throw new AppException(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(),
                    RESOURCE_NOT_FOUND_ERROR_MESSAGE,
                    e);
            }
            if (e.getMessage().contains("Request rate is large")) {
                throw new AppException(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    COSMOS_DB_RATE_LIMIT_ERROR_MESSAGE,
                    e);
            }
            throw new AppException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                RETRIEVING_RESULT_SET_ERROR_MESSAGE,
                e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                RETRIEVING_RESULT_SET_ERROR_MESSAGE,
                e);
        }
        return resultList;
    }

    private boolean isResourceNotFoundException(ExecutionException e) {
        if (e.getCause() instanceof ResponseException responseException) {
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
