package org.opengroup.osdu.entitlements.v2.aws.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.converter.SqlDateReadConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.converter.SqlDateWriteConverter;
import org.opengroup.osdu.entitlements.v2.aws.mongodb.core.helper.BasicMongoDBHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config.MongoConfig.DOT_NAME_REPLACER;

@Configuration
public class MongoConfigTest extends AbstractMongoClientConfiguration {

    @Inject
    MongoProperties props;

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(props.getMongoUri());
    }

    @Override
    public String getDatabaseName() {
        return props.getDbName();
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
