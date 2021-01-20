package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.connection;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.groovy.jsr223.GroovyTranslator;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeEdge;
import org.opengroup.osdu.entitlements.v2.azure.model.NodeVertex;
import org.opengroup.osdu.entitlements.v2.azure.service.VertexUtilService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
public class ClusterGremlinConnector implements GremlinConnector {
    private static final int MAX_CONTENT_LENGTH = 65536;
    private static final int MAX_IN_PROCESS = 16;
    private static final String TRAVERSAL_SUBMIT_ERROR_MESSAGE = "Error submitting traversal";
    private static final String RETRIEVING_RESULT_SET_ERROR_MESSAGE = "Error retrieving ResultSet object";
    private static final String MSI_HOST = "http://169.254.169.254/";
    private static final String MSI_PATH = "metadata/identity/oauth2/token?api-version=2018-02-01&resource=";
    private static final String ARM_HOST = "https://management.azure.com/";
    private static final String ARM_PATH_FORMAT = "subscriptions/%s/resourceGroups/%s/providers/Microsoft.DocumentDb/" +
            "databaseAccounts/%s/listKeys/?api-version=2016-03-31";
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
            String accessKey;
            if (config.hasCosmosDbConfig()) {
                accessKey = getAccessKey(config.getSubscriptionId(), config.getResourceGroup(), config.getCosmosDbAccountName());
            } else {
                accessKey = config.getGremlinPassword();
            }
            return Cluster.build(config.getGremlinEndpoint())
                    .port(config.getGremlinPort())
                    .credentials(config.getGremlinUsername(), accessKey)
                    .enableSsl(config.isGremlinSslEnabled())
                    .maxSimultaneousUsagePerConnection(MAX_IN_PROCESS)
                    .maxInProcessPerConnection(MAX_IN_PROCESS)
                    .maxContentLength(MAX_CONTENT_LENGTH)
                    .serializer(Serializers.GRAPHSON_V2D0.toString())
                    .create();
        } catch (IllegalArgumentException e) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Invalid configuration of Gremlin cluster", e);
        }
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

    private void validateRequestSuccessful(Map<String, Object> statusAttributes) {
        if ((Integer) statusAttributes.get("x-ms-status-code") != 200) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    TRAVERSAL_SUBMIT_ERROR_MESSAGE);
        }
    }

    private String getAccessToken() {
        final String msiEndpoint = MSI_HOST + MSI_PATH + ARM_HOST;
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, msiEndpoint);
        httpRequest.setHeader("Metadata", "true");

        String content = performAadRequest(httpRequest);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
        return jsonObject.get("access_token").getAsString();
    }

    private String getAccessKey(String subscriptionId, String resourceGroup, String cosmosDbAccountName) {
        final String armEndpoint = ARM_HOST + String.format(ARM_PATH_FORMAT,
                subscriptionId, resourceGroup, cosmosDbAccountName);
        HttpRequest httpRequest = new HttpRequest(HttpMethod.POST, armEndpoint);
        httpRequest.setHeader("Authorization", "Bearer " + getAccessToken());

        String content = performAadRequest(httpRequest);
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(content, JsonObject.class);
        return jsonObject.get("primaryMasterKey").getAsString();
    }

    private String performAadRequest(HttpRequest httpRequest) {
        HttpClient httpClient = HttpClient.createDefault();
        Mono<HttpResponse> response = httpClient.send(httpRequest);
        String responseBody;
        try {
            responseBody = Objects.requireNonNull(response.block()).getBodyAsString().block();
        } catch (RuntimeException e) {
            throw new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "Error calling AAD", e);
        }
        return responseBody;
    }
}
