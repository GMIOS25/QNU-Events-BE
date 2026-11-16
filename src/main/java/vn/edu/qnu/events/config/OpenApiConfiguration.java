package vn.edu.qnu.events.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI qnuEventsOpenApi() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Dán accessToken nhận được từ POST /api/v1/auth/login.");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .info(new Info()
                        .title("QNU Events API")
                        .version("v1")
                        .description("Event registration and participation management. "
                                + "Business endpoints will be added as MVP features are implemented."));
    }
}
