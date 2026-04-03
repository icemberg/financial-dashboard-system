package com.financedashboard.zorvyn.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for Swagger UI documentation.
 * Accessible at: /swagger-ui.html
 *
 * Configures:
 * - API metadata (title, version, description)
 * - JWT Bearer authentication scheme (global)
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI zorvynOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title("Zorvyn Finance Dashboard API")
                        .version("1.0.0")
                        .description(
                                "RESTful API for managing financial records, users, and dashboard analytics. "
                                + "Supports JWT authentication, Google OAuth2, role-based access control "
                                + "(VIEWER, ANALYST, ADMIN), paginated queries, and soft-delete operations."
                        )
                        .contact(new Contact()
                                .name("Zorvyn")
                                .email("admin@zorvyn.com")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here. Obtain it from POST /v1/auth/login or /v1/auth/register.")
                        )
                );
    }
}
