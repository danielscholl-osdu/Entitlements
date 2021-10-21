// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.entitlements.v2.aws.mongodb.core.config;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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

/**
 * Basic configuration for MongoDB beans.
 * Will be imported from other services
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    public static final String SQL_TIMESTAMP_REPLACER_NAME = "_sql_timestamp_replacer_";
    public static final String DOT_NAME_REPLACER = "_dot_replacer_";

    @Inject
    MongoProperties props;

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(getMongoURI());
    }

    @Override
    public String getDatabaseName() {
        return props.getDatabaseName();
    }

    // public void setDatabaseName(String databaseName) {
    //     this.databaseName = databaseName;
    // }

    public String getMongoURI() {

        Boolean useSrvEndpoint = Boolean.parseBoolean(props.getUseSrvEndpointStr());

        if (useSrvEndpoint) {

            String srvUriFormat = "mongodb+srv://%s:%s@%s/%s?ssl=%s&retryWrites=%s&w=%s";

            String srvUri = String.format(
                srvUriFormat, 
                props.getUsername(), 
                props.getPassword(), 
                props.getEndpoint(), 
                props.getAuthDatabase(),
                props.getEnableTLS(), 
                props.getRetryWrites(), 
                props.getWriteMode());

            return srvUri;            
        }        
        else {
            String uriFormat = "mongodb://%s:%s@%s/%s?ssl=%s&retryWrites=%s&w=%s";

            String uri = String.format(
                uriFormat, 
                props.getUsername(), 
                props.getPassword(), 
                props.getEndpoint(), 
                props.getAuthDatabase(),
                props.getEnableTLS(), 
                props.getRetryWrites(), 
                props.getWriteMode());

            return uri;
        }
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
