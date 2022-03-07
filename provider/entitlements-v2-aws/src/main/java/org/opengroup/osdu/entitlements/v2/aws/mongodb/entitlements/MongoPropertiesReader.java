// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// Copyright MongoDB, Inc or its affiliates. All Rights Reserved.
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

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import org.opengroup.osdu.core.aws.mongodb.config.MongoProperties;
import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.opengroup.osdu.core.aws.ssm.K8sParameterNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Data
@Component
public class MongoPropertiesReader {

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
    @Value("${osdu.mongodb.database}")
    private String databaseName;
    @Value("${osdu.mongodb.maxPoolSize}")
    private String maxPoolSize;
    @Value("${osdu.mongodb.readPreference}")
    private String readPreference;

    @PostConstruct
    private void init() throws K8sParameterNotFoundException, JsonProcessingException {

        K8sLocalParameterProvider provider = new K8sLocalParameterProvider();

        //fix for local run
        if (!provider.getLocalMode()) {
            Map<String, String> credentials = provider.getCredentialsAsMap("mongodb_credentials");

            if (credentials != null) {
                username = credentials.get("username");
                password = credentials.get("password");
                authDatabase = credentials.get("authDB");
            }
            endpoint = provider.getParameterAsStringOrDefault("mongodb_host", endpoint);
            port = provider.getParameterAsStringOrDefault("mongodb_port", port);
        }

    }

    public MongoProperties getProperties() {
        return MongoProperties.builder()
                .username(username)
                .password(password)
                .endpoint(endpoint)
                .authDatabase(authDatabase)
                .port(port)
                .retryWrites(retryWrites)
                .writeMode(writeMode)
                .useSrvEndpointStr(useSrvEndpointStr)
                .enableTLS(enableTLS)
                .databaseName(databaseName)
                .maxPoolSize(maxPoolSize)
                .readPreference(readPreference)
                .build();
    }
}
