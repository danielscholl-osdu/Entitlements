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

package org.opengroup.osdu.entitlements.v2.aws.mongodb.entitlements;

import org.opengroup.osdu.core.aws.mongodb.MongoDBSimpleFactory;
import org.opengroup.osdu.core.aws.mongodb.helper.BasicMongoDBHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Basic configuration for MongoDB beans.
 * Will be imported from other services
 */
@Configuration
public class MongoConfig {

    @Bean
    @Autowired
    public BasicMongoDBHelper mongoHelper(MongoDBSimpleFactory mongoDBSimpleFactory, MongoPropertiesReader propertiesReader) {
        return mongoDBSimpleFactory.getHelper(propertiesReader.getProperties());
    }
}
