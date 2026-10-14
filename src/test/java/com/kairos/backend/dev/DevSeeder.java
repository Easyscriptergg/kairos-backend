package com.kairos.backend.dev;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.kairos.backend.auth.AuthService;
import com.kairos.backend.auth.UserAccountRepository;
import com.kairos.backend.training.TrainingService;
import com.kairos.backend.training.TrainingSessionDto;

/**
 * Repeatable development seed for the {@code local} profile only: creates
 * one clearly named demo account with a week of sessions the first time the
 * local database starts, and does nothing on later starts. It is in the test
 * sources, so it cannot run in production.
 */
@Component
@Profile("local")
class DevSeeder implements ApplicationRunner {

    static final String EMAIL = "demo@kairos.local";
    static final String PASSWORD = "kairos-local-demo";

    private static final Logger log = LoggerFactory.getLogger(DevSeeder.class);

    private final UserAccountRepository users;
    private final AuthService auth;
    private final TrainingService training;

    DevSeeder(UserAccountRepository users, AuthService auth, TrainingService training) {
        this.users = users;
        this.auth = auth;
        this.training = training;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByEmailIgnoreCase(EMAIL).isPresent()) {
            log.info("Local demo account already exists: {}", EMAIL);
            return;
        }
        UUID userId = auth.register(EMAIL, PASSWORD).user().id();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String[][] week = {
            {"Upper Body A", "bench-press", "100"},
            {"Lower Body A", "barbell-squat", "120"},
            {"Upper Body B", "overhead-press", "60"},
        };
        for (int i = 0; i < week.length; i++) {
            LocalDate date = today.minusDays(6 - i * 2L);
            String id = "seed-" + date;
            List<TrainingSessionDto.SetDto> sets = new ArrayList<>();
            for (int s = 1; s <= 4; s++) {
                sets.add(new TrainingSessionDto.SetDto(id + "-s" + s, s, Double.valueOf(week[i][2]), 5, 7.0 + s * 0.5,
                    180, date.atTime(17, 30).toInstant(ZoneOffset.UTC).plusSeconds(s * 240L)));
            }
            training.create(userId, new TrainingSessionDto(id, date, week[i][0], "strength", 60,
                List.of(new TrainingSessionDto.ExerciseDto(id + "-ex", week[i][1], sets))));
        }
        log.info("Seeded local demo account {} (password: {}) with {} sessions at {}", EMAIL, PASSWORD, week.length, Instant.now());
    }
}
