package com.kairos.backend.nutrition;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * One day of eating, keyed by the athlete's local calendar day. The day's
 * totals are the sum of its meals and are calculated, never stored.
 */
@Entity
@Table(name = "nutrition_day")
public class NutritionDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "client_id", nullable = false)
    String clientId;

    @Column(name = "day_date", nullable = false)
    LocalDate dayDate;

    @Column(name = "hydration_liters")
    Double hydrationLiters;

    @Column(name = "target_calories")
    int targetCalories;

    @Column(name = "target_protein")
    int targetProtein;

    @Column(name = "target_carbohydrates")
    int targetCarbohydrates;

    @Column(name = "target_fat")
    int targetFat;

    @Column(name = "target_hydration")
    double targetHydration;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    List<Meal> meals = new ArrayList<>();
}
