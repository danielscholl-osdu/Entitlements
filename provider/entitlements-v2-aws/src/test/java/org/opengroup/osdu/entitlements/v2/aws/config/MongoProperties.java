package org.opengroup.osdu.entitlements.v2.aws.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class MongoProperties {

    @Value("${osdu.mongodb.uri}")
    private String mongoUri;

    @Value("${osdu.mongodb.database.name}")
    private String dbName;
}
