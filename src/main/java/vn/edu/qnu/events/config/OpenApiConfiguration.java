package vn.edu.qnu.events.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI qnuEventsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("QNU Events API")
                .version("v1")
                .description("Event registration and participation management. "
                        + "Business endpoints will be added as MVP features are implemented."));
    }
}
