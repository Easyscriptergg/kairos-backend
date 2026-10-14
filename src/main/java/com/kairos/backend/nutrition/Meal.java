package com.kairos.backend.nutrition;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "meal")
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutrition_day_id")
    NutritionDay day;

    @Column(name = "client_id", nullable = false)
    String clientId;

    String name;

    @Column(name = "meal_time")
    String mealTime;

    int calories;
    int protein;
    int carbohydrates;
    int fat;
    short position;
}
