package com.kairos.backend.nutrition;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kairos.backend.auth.CurrentUser;
import com.kairos.backend.common.ApiException;
import com.kairos.backend.common.DateRanges;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/nutrition-days")
@Tag(name = "Nutrition days")
public class NutritionController {

    private final NutritionDayRepository days;

    public NutritionController(NutritionDayRepository days) {
        this.days = days;
    }

    @GetMapping
    @Operation(summary = "The athlete's nutrition days with their meals, oldest first, optionally within [from, to]")
    @Transactional(readOnly = true)
    public List<NutritionDayDto> list(
        @CurrentUser UUID userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = DateRanges.fromOrEarliest(from);
        LocalDate end = DateRanges.toOrLatest(to);
        DateRanges.requireOrdered(start, end);
        return days.findForUser(userId, start, end).stream().map(NutritionDayDto::of).toList();
    }

    /** Replaces the whole day, meals included, so adding a meal is a read-modify-write of the day. */
    @PutMapping("/{date}")
    @Operation(summary = "Create or replace a day, including all of its meals")
    @Transactional
    public NutritionDayDto put(
        @CurrentUser UUID userId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Valid @RequestBody NutritionDayDto body) {
        if (!date.equals(body.date())) {
            throw ApiException.badRequest("The date in the path and the body must match.");
        }
        DateRanges.requirePastOrToday(date);

        NutritionDay day = days.findByUserIdAndDayDate(userId, date).orElseGet(NutritionDay::new);
        day.userId = userId;
        day.clientId = body.id();
        day.dayDate = date;
        day.hydrationLiters = body.hydrationLiters();
        day.targetCalories = body.targets().calories();
        day.targetProtein = body.targets().protein();
        day.targetCarbohydrates = body.targets().carbohydrates();
        day.targetFat = body.targets().fat();
        day.targetHydration = body.targets().hydration();

        day.meals.clear();
        short position = 0;
        for (NutritionDayDto.MealDto dto : body.meals()) {
            Meal meal = new Meal();
            meal.day = day;
            meal.clientId = dto.id();
            meal.name = dto.name().trim();
            meal.mealTime = dto.time();
            meal.calories = dto.calories();
            meal.protein = dto.protein();
            meal.carbohydrates = dto.carbohydrates();
            meal.fat = dto.fat();
            meal.position = position++;
            day.meals.add(meal);
        }
        return NutritionDayDto.of(days.saveAndFlush(day));
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a day and its meals")
    @Transactional
    public void delete(@CurrentUser UUID userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        NutritionDay day = days.findByUserIdAndDayDate(userId, date)
            .orElseThrow(() -> ApiException.notFound("A nutrition day for " + date));
        days.delete(day);
    }
}

@Repository
interface NutritionDayRepository extends JpaRepository<NutritionDay, UUID> {

    Optional<NutritionDay> findByUserIdAndDayDate(UUID userId, LocalDate dayDate);

    @Query("select d from NutritionDay d where d.userId = :userId and d.dayDate between :from and :to order by d.dayDate")
    List<NutritionDay> findForUser(UUID userId, LocalDate from, LocalDate to);
}
