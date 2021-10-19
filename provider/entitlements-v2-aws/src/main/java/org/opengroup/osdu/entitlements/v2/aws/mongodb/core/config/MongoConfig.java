package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
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

/**
 * Basic configuration for MongoDB beans.
 * Will be imported from other services
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    public static final String SQL_TIMESTAMP_REPLACER_NAME = "_sql_timestamp_replacer_";
    public static final String DOT_NAME_REPLACER = "_dot_replacer_";

    @Value("${osdu.mongodb.uri}")
    private String mongoURI;
    @Value("${osdu.mongodb.database}")
    private String databaseName;

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(mongoURI);
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setMongoURI(String mongoURI) {
        this.mongoURI = mongoURI;
    }

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
