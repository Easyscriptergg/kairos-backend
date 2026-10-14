package com.kairos.backend.training;

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
 * A logged session. Sets belong to the exercise they were performed on, and
 * exercises to the session, so a set cannot be attributed to the wrong
 * exercise or workout.
 */
@Entity
@Table(name = "training_session")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "client_id", nullable = false)
    String clientId;

    @Column(name = "session_date", nullable = false)
    LocalDate sessionDate;

    String name;
    String type;

    @Column(name = "duration_minutes")
    int durationMinutes;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    List<TrainingExercise> exercises = new ArrayList<>();
}
