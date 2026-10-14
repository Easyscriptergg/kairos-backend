package com.kairos.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** One athlete's records are never visible to, or changeable by, another. */
class IsolationAndProfileTest extends IntegrationTest {

    private static final Map<String, Object> PROFILE = Map.of(
        "name", "Alex", "sport", "strength", "discipline", "Powerlifting", "experience", "advanced",
        "goal", "Total 600 kg", "trainingFrequency", 4, "equipment", List.of("barbell", "bench"),
        "performanceLevel", "competitive", "bodyMetrics", Map.of("height", 180.0, "weight", 90.5, "age", 29));

    @Test
    void profileIsAbsentUntilSavedThenRoundTrips() throws Exception {
        String token = newAthlete();
        mvc.perform(get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(PROFILE)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alex"))
            .andExpect(jsonPath("$.equipment", hasSize(2)))
            .andExpect(jsonPath("$.bodyMetrics.weight").value(90.5))
            .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    void athletesCannotSeeOrChangeEachOthersRecords() throws Exception {
        String alice = newAthlete();
        String bob = newAthlete();

        mvc.perform(post("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(alice))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(session("shared-client-id", LocalDate.of(2026, 10, 11)))))
            .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, bearer(alice))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(PROFILE)))
            .andExpect(status().isOk());

        // Bob sees nothing of Alice's
        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(bob)))
            .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/profile").header(HttpHeaders.AUTHORIZATION, bearer(bob)))
            .andExpect(status().isNotFound());

        // Bob cannot delete Alice's session, even knowing its id
        mvc.perform(delete("/api/v1/training-sessions/shared-client-id").header(HttpHeaders.AUTHORIZATION, bearer(bob)))
            .andExpect(status().isNotFound());

        // Bob reusing the same client id creates his own, separate session
        mvc.perform(post("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(bob))
                .contentType(MediaType.APPLICATION_JSON).content(toJson(session("shared-client-id", LocalDate.of(2026, 10, 12)))))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(alice)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].date").value("2026-10-11"));
        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(bob)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].date").value("2026-10-12"));
    }
}
