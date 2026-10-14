package com.kairos.backend.training;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kairos.backend.common.ApiException;
import com.kairos.backend.common.DateRanges;

@Service
public class TrainingService {

    private final TrainingSessionRepository sessions;
    private final ExerciseRepository exercises;

    TrainingService(TrainingSessionRepository sessions, ExerciseRepository exercises) {
        this.sessions = sessions;
        this.exercises = exercises;
    }

    public record CreateResult(TrainingSessionDto session, boolean created) {
    }

    @Transactional(readOnly = true)
    public List<TrainingSessionDto> list(UUID userId, LocalDate from, LocalDate to) {
        LocalDate start = DateRanges.fromOrEarliest(from);
        LocalDate end = DateRanges.toOrLatest(to);
        DateRanges.requireOrdered(start, end);
        return sessions.findForUser(userId, start, end).stream().map(TrainingSessionDto::of).toList();
    }

    /**
     * Stores a session. Posting the same client id again returns the stored
     * session unchanged instead of creating a duplicate.
     */
    @Transactional
    public CreateResult create(UUID userId, TrainingSessionDto dto) {
        var existing = sessions.findByUserIdAndClientId(userId, dto.id());
        if (existing.isPresent()) {
            return new CreateResult(TrainingSessionDto.of(existing.get()), false);
        }
        DateRanges.requirePastOrToday(dto.date());

        Set<String> requested = dto.exercises().stream().map(TrainingSessionDto.ExerciseDto::exerciseId).collect(Collectors.toSet());
        Map<String, Exercise> known = exercises.findAllById(requested).stream()
            .collect(Collectors.toMap(Exercise::getId, Function.identity()));
        Set<String> unknown = new HashSet<>(requested);
        unknown.removeAll(known.keySet());
        if (!unknown.isEmpty()) {
            throw ApiException.badRequest("Unknown exercise id(s): " + String.join(", ", unknown.stream().sorted().toList()));
        }

        TrainingSession session = new TrainingSession();
        session.userId = userId;
        session.clientId = dto.id();
        session.sessionDate = dto.date();
        session.name = dto.name().trim();
        session.type = dto.type();
        session.durationMinutes = dto.durationMinutes();

        short position = 0;
        for (TrainingSessionDto.ExerciseDto exerciseDto : dto.exercises()) {
            TrainingExercise exercise = new TrainingExercise();
            exercise.session = session;
            exercise.clientId = exerciseDto.id();
            exercise.exercise = known.get(exerciseDto.exerciseId());
            exercise.position = position++;
            for (TrainingSessionDto.SetDto setDto : exerciseDto.sets()) {
                TrainingSet set = new TrainingSet();
                set.trainingExercise = exercise;
                set.clientId = setDto.id();
                set.setNumber = setDto.setNumber().shortValue();
                set.weightKg = setDto.weightKg();
                set.reps = setDto.reps().shortValue();
                set.rpe = setDto.rpe();
                set.restSeconds = setDto.restSeconds();
                set.performedAt = setDto.performedAt();
                exercise.sets.add(set);
            }
            session.exercises.add(exercise);
        }
        return new CreateResult(TrainingSessionDto.of(sessions.saveAndFlush(session)), true);
    }

    @Transactional
    public void delete(UUID userId, String clientId) {
        TrainingSession session = sessions.findByUserIdAndClientId(userId, clientId)
            .orElseThrow(() -> ApiException.notFound("The training session"));
        sessions.delete(session);
    }
}
