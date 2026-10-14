package com.kairos.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Base class for tests that run the whole application against a real
 * PostgreSQL server (started once per JVM from the embedded binaries), so
 * Flyway migrations, constraints and queries are exercised exactly as in
 * production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    protected static final EmbeddedPostgres POSTGRES = startPostgres();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    /** Registers a fresh account and returns its bearer token. */
    protected String newAthlete() throws Exception {
        String email = "athlete-" + UUID.randomUUID() + "@example.com";
        String body = mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("email", email, "password", "correct horse battery"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected JsonNode readJson(String body) throws IOException {
        return json.readTree(body);
    }

    /** A session with two exercises; the second exercise's sets must stay attached to it. */
    protected static Map<String, Object> session(String id, LocalDate date) {
        return Map.of(
            "id", id,
            "date", date.toString(),
            "name", "Upper Body A",
            "type", "strength",
            "durationMinutes", 62,
            "exercises", List.of(
                Map.of("id", id + "-ex1", "exerciseId", "bench-press", "sets", List.of(
                    set(id + "-s1", 1, 100, 5, 8.0),
                    set(id + "-s2", 2, 100, 5, 8.5))),
                Map.of("id", id + "-ex2", "exerciseId", "barbell-row", "sets", List.of(
                    set(id + "-s3", 1, 80, 8, 7.0)))));
    }

    private static Map<String, Object> set(String id, int number, double weight, int reps, double rpe) {
        return Map.of("id", id, "setNumber", number, "weightKg", weight, "reps", reps, "rpe", rpe,
            "performedAt", "2026-10-13T17:30:00Z");
    }
}
