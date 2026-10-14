package com.kairos.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.kairos.backend.auth.UserAccountRepository;

class AuthTest extends IntegrationTest {

    @Autowired
    UserAccountRepository users;

    private org.springframework.test.web.servlet.ResultActions register(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(toJson(Map.of("email", email, "password", password))));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(toJson(Map.of("email", email, "password", password))));
    }

    @Test
    void registerLoginAndMe() throws Exception {
        String email = "Runner-" + UUID.randomUUID() + "@Example.com";
        register(email, "a long enough password")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.expiresAt").isString())
            .andExpect(jsonPath("$.user.email").value(email.toLowerCase()));

        String token = readJson(login(email.toUpperCase(), "a long enough password")
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email.toLowerCase()));
    }

    @Test
    void passwordsAreStoredHashed() throws Exception {
        String email = "hash-" + UUID.randomUUID() + "@example.com";
        register(email, "plain text never stored").andExpect(status().isCreated());
        String hash = users.findByEmailIgnoreCase(email).orElseThrow().getPasswordHash();
        assertThat(hash).startsWith("{bcrypt}").doesNotContain("plain text never stored");
    }

    @Test
    void duplicateEmailsAreRejectedIgnoringCase() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        register(email, "a long enough password").andExpect(status().isCreated());
        register(email.toUpperCase(), "another long password").andExpect(status().isConflict());
    }

    @Test
    void weakPasswordsAndBadEmailsAreRejectedWithFieldErrors() throws Exception {
        register("someone-" + UUID.randomUUID() + "@example.com", "short")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("password"));
        register("not-an-email", "a long enough password")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        register(email, "a long enough password").andExpect(status().isCreated());
        String wrongPassword = login(email, "the wrong password").andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("nobody-" + UUID.randomUUID() + "@example.com", "whatever password").andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();
        assertThat(readJson(wrongPassword).get("detail")).isEqualTo(readJson(unknownEmail).get("detail"));
    }
}
