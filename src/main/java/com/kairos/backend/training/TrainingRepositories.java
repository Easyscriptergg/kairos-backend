package com.kairos.backend.training;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {

    Optional<TrainingSession> findByUserIdAndClientId(UUID userId, String clientId);

    @Query("""
        select s from TrainingSession s
        where s.userId = :userId and s.sessionDate between :from and :to
        order by s.sessionDate, s.id""")
    List<TrainingSession> findForUser(UUID userId, LocalDate from, LocalDate to);
}

interface ExerciseRepository extends JpaRepository<Exercise, String> {
}
