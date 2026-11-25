package vn.edu.qnu.events.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityConfigurationTest.SecurityTestController.class, properties = {
        "app.cors.allowed-origins=http://localhost:5173",
        "app.security.jwt-secret=test-jwt-secret-key-must-be-at-least-32-bytes-long!",
        "springdoc.api-docs.enabled=false"
})
@Import({SecurityConfiguration.class, ClockConfiguration.class})
class SecurityConfigurationTest {

    @RestController
    static class SecurityTestController {
        @GetMapping("/api/v1/events")
        public String getEvents() {
            return "events";
        }

        @PostMapping("/api/v1/events")
        public String postEvents() {
            return "created";
        }
    }

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

    @Test
    void unauthorizedAndForbiddenResponsesSerializeTimestampAsIso8601() throws Exception {
        String iso8601Regex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$";
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.timestamp", matchesPattern(iso8601Regex)));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    void forbiddenResponseSerializesTimestampAsIso8601() throws Exception {
        String iso8601Regex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$";
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.timestamp", matchesPattern(iso8601Regex)));
    }

    @Test
    void jwtSecretFailsFastOutsideDevDemoTestProfiles() {
        MockEnvironment prodEnv = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        prodEnv.setActiveProfiles("prod");
        assertThatThrownBy(() -> SecurityConfiguration.resolveJwtSecret("", prodEnv))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 byte");

        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");
        assertThat(SecurityConfiguration.resolveJwtSecret("", devEnv))
                .isEqualTo(SecurityConfiguration.DEV_FALLBACK_JWT_SECRET);
    }
}
