package com.kairos.backend.training;

import java.time.Instant;
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
@Table(name = "training_set")
public class TrainingSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_exercise_id")
    TrainingExercise trainingExercise;

    @Column(name = "client_id", nullable = false)
    String clientId;

    @Column(name = "set_number")
    short setNumber;

    @Column(name = "weight_kg")
    double weightKg;

    short reps;

    Double rpe;

    @Column(name = "rest_seconds")
    Integer restSeconds;

    @Column(name = "performed_at")
    Instant performedAt;
}
