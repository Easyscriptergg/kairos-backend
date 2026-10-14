package com.kairos.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class TrainingTest extends IntegrationTest {

    private ResultActions create(String token, Object body) throws Exception {
        return mvc.perform(post("/api/v1/training-sessions")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(toJson(body)));
    }

    @Test
    void createdSessionsAreStoredWithSetsOnTheRightExercise() throws Exception {
        String token = newAthlete();
        create(token, session("workout-a", LocalDate.of(2026, 10, 12)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("workout-a"));

        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].date").value("2026-10-12"))
            .andExpect(jsonPath("$[0].durationMinutes").value(62))
            .andExpect(jsonPath("$[0].exercises[0].exerciseId").value("bench-press"))
            .andExpect(jsonPath("$[0].exercises[0].sets", hasSize(2)))
            .andExpect(jsonPath("$[0].exercises[0].sets[1].rpe").value(8.5))
            .andExpect(jsonPath("$[0].exercises[1].exerciseId").value("barbell-row"))
            .andExpect(jsonPath("$[0].exercises[1].sets", hasSize(1)))
            .andExpect(jsonPath("$[0].exercises[1].sets[0].weightKg").value(80.0))
            .andExpect(jsonPath("$[0].exercises[1].sets[0].reps").value(8));
    }

    @Test
    void repeatingACreateIsIdempotent() throws Exception {
        String token = newAthlete();
        create(token, session("workout-retry", LocalDate.of(2026, 10, 10))).andExpect(status().isCreated());
        create(token, session("workout-retry", LocalDate.of(2026, 10, 10))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void dateRangeFiltersSessions() throws Exception {
        String token = newAthlete();
        create(token, session("w-1", LocalDate.of(2026, 9, 1))).andExpect(status().isCreated());
        create(token, session("w-2", LocalDate.of(2026, 10, 1))).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/training-sessions?from=2026-09-15&to=2026-10-31").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value("w-2"));
        mvc.perform(get("/api/v1/training-sessions?from=2026-10-31&to=2026-09-01").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidSessionsAreRejected() throws Exception {
        String token = newAthlete();

        Map<String, Object> unknownExercise = new HashMap<>(session("w-bad", LocalDate.of(2026, 10, 1)));
        unknownExercise.put("exercises", List.of(Map.of("id", "x", "exerciseId", "made-up-lift",
            "sets", List.of(Map.of("id", "s", "setNumber", 1, "weightKg", 50, "reps", 5, "performedAt", "2026-10-01T10:00:00Z")))));
        create(token, unknownExercise).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Unknown exercise id(s): made-up-lift"));

        create(token, session("w-future", LocalDate.now().plusDays(5))).andExpect(status().isBadRequest());

        Map<String, Object> noSets = new HashMap<>(session("w-empty", LocalDate.of(2026, 10, 1)));
        noSets.put("exercises", List.of());
        noSets.put("durationMinutes", 0);
        create(token, noSets).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    void deleteRemovesTheSessionAndItsSets() throws Exception {
        String token = newAthlete();
        create(token, session("w-del", LocalDate.of(2026, 10, 2))).andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/training-sessions/w-del").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/training-sessions/w-del").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/training-sessions").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
