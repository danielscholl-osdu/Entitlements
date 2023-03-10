package org.opengroup.osdu.entitlements.v2.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@OpenAPIDefinition(
        info = @Info(
                title = "${api.title}",
                description = "${api.description}",
                version = "${api.version}",
                contact = @Contact(name = "${api.contact.name}", email = "${api.contact.email}"),
                license = @License(name = "${api.license.name}", url = "${api.license.url}")),
        servers = @Server(url = "${api.server.url}"),
        security = @SecurityRequirement(name = "Authorization"),
        tags = {
                @Tag(name = "create-group-api", description = "Create Group API"),
                @Tag(name = "update-group-api", description = "Update Group API"),
                @Tag(name = "delete-group-api", description = "Delete Group API"),
                @Tag(name = "list-group-api", description = "List Group API"),
                @Tag(name = "list-group-on-behalf-of-api", description = "List Group On Behalf Of API"),
                @Tag(name = "add-member-api", description = "Add Member API"),
                @Tag(name = "list-member-api", description = "List Member API"),
                @Tag(name = "delete-member-api", description = "Delete Member API"),
                @Tag(name = "remove-member-api", description = "Remove Member API"),
                @Tag(name = "init-api", description = "Init API"),
                @Tag(name = "health-checks-api", description = "Health Checks API"),
                @Tag(name = "info", description = "Version info endpoint")
        }
)
@SecurityScheme(name = "Authorization", scheme = "bearer", bearerFormat = "Authorization", type = SecuritySchemeType.HTTP)
@Configuration
@Profile("!noswagger")
public class SwaggerConfiguration {

    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            Parameter dataPartitionId = new Parameter()
                    .name(DpsHeaders.DATA_PARTITION_ID)
                    .description("Tenant Id")
                    .in("header")
                    .required(true)
                    .schema(new StringSchema());
            return operation.addParametersItem(dataPartitionId);
        };
    }
}
