package com.kairos.backend.nutrition;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kairos.backend.common.ClientIds;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Wire shape of a nutrition day (frontend: NutritionDayDto in src/data/api/contract.ts).
 * {@code hydrationLiters} is always sent, as null when hydration was not logged, so the
 * client can tell "not logged" from zero.
 */
public record NutritionDayDto(
    @NotNull @Pattern(regexp = ClientIds.PATTERN, message = ClientIds.MESSAGE) String id,
    @NotNull LocalDate date,
    @JsonInclude(JsonInclude.Include.ALWAYS) @DecimalMin("0") @DecimalMax("20") Double hydrationLiters,
    @NotNull @Size(max = 30) List<@Valid @NotNull MealDto> meals,
    @NotNull @Valid Targets targets) {

    public record MealDto(
        @NotNull @Pattern(regexp = ClientIds.PATTERN, message = ClientIds.MESSAGE) String id,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Pattern(regexp = "([01]\\d|2[0-3]):[0-5]\\d", message = "must be a 24-hour time, HH:MM") String time,
        @NotNull @Min(0) @Max(20000) Integer calories,
        @NotNull @Min(0) @Max(2000) Integer protein,
        @NotNull @Min(0) @Max(3000) Integer carbohydrates,
        @NotNull @Min(0) @Max(2000) Integer fat) {
    }

    public record Targets(
        @NotNull @Min(1) @Max(20000) Integer calories,
        @NotNull @Min(0) @Max(2000) Integer protein,
        @NotNull @Min(0) @Max(3000) Integer carbohydrates,
        @NotNull @Min(0) @Max(2000) Integer fat,
        @NotNull @DecimalMin("0") @DecimalMax("20") Double hydration) {
    }

    static NutritionDayDto of(NutritionDay d) {
        return new NutritionDayDto(
            d.clientId,
            d.dayDate,
            d.hydrationLiters,
            d.meals.stream().map(m -> new MealDto(m.clientId, m.name, m.mealTime, m.calories, m.protein, m.carbohydrates, m.fat)).toList(),
            new Targets(d.targetCalories, d.targetProtein, d.targetCarbohydrates, d.targetFat, d.targetHydration));
    }
}
