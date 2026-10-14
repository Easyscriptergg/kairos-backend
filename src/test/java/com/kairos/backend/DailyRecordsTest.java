package com.kairos.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Recovery records and nutrition days: one per athlete per calendar day, replaced on PUT. */
class DailyRecordsTest extends IntegrationTest {

    private ResultActions putJson(String token, String path, Object body) throws Exception {
        return mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
    }

    private static Map<String, Object> recovery(String date, double sleep, int restingHr) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", "recovery-" + date);
        body.put("date", date);
        body.put("sleepDurationHours", sleep);
        body.put("sleepQuality", 80);
        body.put("restingHeartRate", restingHr);
        body.put("hrvMs", 60);
        body.put("soreness", 3);
        body.put("source", Map.of("provider", "manual", "recordedAt", date + "T07:05:00Z"));
        return body;
    }

    @Test
    void recoveryCheckInIsReplacedNotDuplicated() throws Exception {
        String token = newAthlete();
        putJson(token, "/api/v1/recovery-records/2026-10-13", recovery("2026-10-13", 7.0, 60)).andExpect(status().isOk());
        putJson(token, "/api/v1/recovery-records/2026-10-13", recovery("2026-10-13", 8.2, 55))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sleepDurationHours").value(8.2));

        mvc.perform(get("/api/v1/recovery-records").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].restingHeartRate").value(55))
            .andExpect(jsonPath("$[0].source.provider").value("manual"))
            .andExpect(jsonPath("$[0].mood").doesNotExist());
    }

    @Test
    void recoveryValidation() throws Exception {
        String token = newAthlete();
        putJson(token, "/api/v1/recovery-records/2026-10-12", recovery("2026-10-13", 7.0, 60))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("The date in the path and the body must match."));

        Map<String, Object> outOfRange = recovery("2026-10-13", 30, 10);
        putJson(token, "/api/v1/recovery-records/2026-10-13", outOfRange)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasSize(2)));

        Map<String, Object> imported = recovery("2026-10-13", 7.5, 58);
        imported.put("source", Map.of("provider", "garmin", "recordedAt", "2026-10-13T07:00:00Z"));
        putJson(token, "/api/v1/recovery-records/2026-10-13", imported).andExpect(status().isBadRequest());
    }

    @Test
    void nutritionDayKeepsMealsInOrderAndReportsUnloggedHydrationAsNull() throws Exception {
        String token = newAthlete();
        Map<String, Object> day = new HashMap<>();
        day.put("id", "nutrition-2026-10-13");
        day.put("date", "2026-10-13");
        day.put("hydrationLiters", null);
        day.put("meals", List.of(
            Map.of("id", "m1", "name", "Oats and whey", "time", "07:30", "calories", 520, "protein", 40, "carbohydrates", 60, "fat", 12),
            Map.of("id", "m2", "name", "Chicken and rice", "time", "12:45", "calories", 750, "protein", 55, "carbohydrates", 90, "fat", 15)));
        day.put("targets", Map.of("calories", 2600, "protein", 165, "carbohydrates", 320, "fat", 75, "hydration", 3.0));

        putJson(token, "/api/v1/nutrition-days/2026-10-13", day)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hydrationLiters").value(nullValue()))
            .andExpect(jsonPath("$.meals", hasSize(2)))
            .andExpect(jsonPath("$.meals[1].name").value("Chicken and rice"));

        // Replacing the day with one meal removes the other
        day.put("meals", List.of(Map.of("id", "m1", "name", "Oats and whey", "time", "07:30", "calories", 520, "protein", 40, "carbohydrates", 60, "fat", 12)));
        day.put("hydrationLiters", 2.5);
        putJson(token, "/api/v1/nutrition-days/2026-10-13", day).andExpect(status().isOk());

        mvc.perform(get("/api/v1/nutrition-days").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].meals", hasSize(1)))
            .andExpect(jsonPath("$[0].hydrationLiters").value(2.5));

        Map<String, Object> badMeal = new HashMap<>(day);
        badMeal.put("meals", List.of(Map.of("id", "m9", "name", "Late snack", "time", "25:00", "calories", -5, "protein", 1, "carbohydrates", 1, "fat", 1)));
        putJson(token, "/api/v1/nutrition-days/2026-10-13", badMeal)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors", hasSize(2)));

        mvc.perform(delete("/api/v1/nutrition-days/2026-10-13").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/nutrition-days").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
