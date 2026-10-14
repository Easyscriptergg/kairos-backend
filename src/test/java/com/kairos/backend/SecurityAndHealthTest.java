package com.kairos.backend;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class SecurityAndHealthTest extends IntegrationTest {

    @Test
    void healthReportsTheDatabase() throws Exception {
        mvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database").value("UP"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void privateEndpointsRequireAToken() throws Exception {
        for (String path : new String[] {"/api/v1/training-sessions", "/api/v1/recovery-records", "/api/v1/nutrition-days", "/api/v1/profile", "/api/v1/auth/me"}) {
            mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    @Test
    void forgedOrMalformedTokensAreRejected() throws Exception {
        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized());
        // Signed with a different key
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAiLCJleHAiOjQxMDI0NDQ4MDB9.c2lnbmF0dXJlLW5vdC12YWxpZC1mb3ItdGhpcy1rZXk";
        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/v1/training-sessions")
                .header(HttpHeaders.ORIGIN, "https://kairos-test.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://kairos-test.example"))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));

        mvc.perform(options("/api/v1/training-sessions")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .andExpect(status().isForbidden());
    }

    @Test
    void openApiDocumentIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/training-sessions']").exists())
            .andExpect(jsonPath("$.paths['/api/v1/recovery-records/{date}']").exists());
    }
}
