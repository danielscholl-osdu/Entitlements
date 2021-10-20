package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.converter.SqlDateReadConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.converter.SqlDateWriteConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper.BasicMongoDBHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

/**
 * Basic configuration for MongoDB beans.
 * Will be imported from other services
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    public static final String SQL_TIMESTAMP_REPLACER_NAME = "_sql_timestamp_replacer_";
    public static final String DOT_NAME_REPLACER = "_dot_replacer_";

    @Value("${osdu.mongodb.username}")
    private String username;
    @Value("${osdu.mongodb.password}")
    private String password;
    @Value("${osdu.mongodb.endpoint}")
    private String endpoint;
    @Value("${osdu.mongodb.authDatabase}")
    private String authDatabase;
    @Value("${osdu.mongodb.port}")
    private String port;
    @Value("${osdu.mongodb.retryWrites}")
    private String retryWrites;
    @Value("${osdu.mongodb.writeMode}")
    private String writeMode;
    @Value("${osdu.mongodb.useSrvEndpoint}")
    private String useSrvEndpointStr;
    @Value("${osdu.mongodb.enableTLS}")
    private String enableTLS;
    
    @PostConstruct
    private void init() throws K8sParameterNotFoundException, JsonProcessingException {
        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();

        if (!provider.getLocalMode()) {
            Map<String,String> credentials = provider.getCredentialsAsMap("mongodb_credentials");

            if (credentials != null) {
                username = credentials.get("username");
                password = credentials.get("password");
                authDatabase = credentials.get("authDB");
            }

            endpoint = provider.getParameterAsStringOrDefault("mongodb_host", endpoint);
            port = provider.getParameterAsStringOrDefault("mongodb_port", port);

        }
    }

    //TODO: use partition to decide DB?
    @Value("${osdu.mongodb.database}")
    private String databaseName;

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(getMongoURI());
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getMongoURI() {

        Boolean useSrvEndpoint = Boolean.parseBoolean(useSrvEndpointStr);

        if (useSrvEndpoint) {

            String srvUriFormat = "mongodb+srv://%s:%s@%s/%s?ssl=%s&retryWrites=%s&w=%s";

            String srvUri = String.format(
                srvUriFormat, 
                username, 
                password, 
                endpoint, 
                authDatabase,
                enableTLS, 
                retryWrites, 
                writeMode);

            return srvUri;            
        }        
        else {
            String uriFormat = "mongodb://%s:%s@%s/%s?ssl=%s&retryWrites=%s&w=%s";

            String uri = String.format(
                uriFormat, 
                username, 
                password, 
                endpoint, 
                authDatabase,
                enableTLS, 
                retryWrites, 
                writeMode);

            return uri;
        }
    }
    //mongodb://admin:Ymbh%3D48IdQ%26~-XyR@mongodb-0.mongodb-svc.mongodb.svc.cluster.local:27017,mongodb-1.mongodb-svc.mongodb.svc.cluster.local:27017,mongodb-2.mongodb-svc.mongodb.svc.cluster.local:27017/admin?ssl=false

    @Bean
    @Autowired
    public BasicMongoDBHelper mongoHelper(MongoTemplate mongoTemplate) {
        return new BasicMongoDBHelper(mongoTemplate);
    }

    @Autowired
    public MappingMongoConverter mongoConverter(MongoCustomConversions customConversions) {
        MongoDatabaseFactory mongoFactory = new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoFactory);
        MappingMongoConverter mongoConverter = new MappingMongoConverter(dbRefResolver, new MongoMappingContext());
        mongoConverter.setMapKeyDotReplacement(DOT_NAME_REPLACER);
        mongoConverter.setCustomConversions(customConversions);
        return mongoConverter;
    }

    @Bean
    @Override
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<>();
        converterList.add(new SqlDateReadConverter());
        converterList.add(new SqlDateWriteConverter());
        return new MongoCustomConversions(converterList);
    }

}
