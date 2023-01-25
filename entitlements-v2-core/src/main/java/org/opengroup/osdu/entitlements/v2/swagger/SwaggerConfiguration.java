package org.opengroup.osdu.entitlements.v2.swagger;

import java.util.Collections;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@Profile("!noswagger")
public class SwaggerConfiguration {

    @Bean
    public OpenAPI openApi(ServletContext servletContext) {
        Server server = new Server().url(servletContext.getContextPath());
        return new OpenAPI()
            .servers(Collections.singletonList(server))
            .info(new Info()
                .title("Entitlements Service")
                .version("1.0"))
            .components(new Components()
                .addSecuritySchemes("Authorization",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Authorization")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")))
            .addSecurityItem(
                new SecurityRequirement()
                    .addList("Authorization"));
    }
}
