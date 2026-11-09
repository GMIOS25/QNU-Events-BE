package vn.edu.qnu.events.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "springdoc.api-docs.enabled=false"
})
@Import({SecurityConfiguration.class, ClockConfiguration.class})
class SecurityConfigurationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void anonymousRequestsCannotAccessBusinessEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/events")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void evenAuthenticatedRequestsAreDeniedUntilExplicitRulesExist() throws Exception {
        mockMvc.perform(get("/api/v1/events")).andExpect(status().isForbidden());
    }

    @Test
    void apiDocumentationIsPrivateByDefault() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }

    @Test
    void configuredFrontendCanPreflightBearerRequests() throws Exception {
        mockMvc.perform(options("/api/v1/events")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void unconfiguredOriginsAreRejected() throws Exception {
        mockMvc.perform(options("/api/v1/events")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
