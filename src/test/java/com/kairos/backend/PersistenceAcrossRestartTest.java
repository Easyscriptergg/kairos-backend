package com.kairos.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.kairos.backend.auth.AuthService;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Stops the application and starts a new instance against the same
 * PostgreSQL database: what was written must still be there, and the
 * already-applied migrations must validate rather than re-run.
 */
class PersistenceAcrossRestartTest {

    @Test
    void writesSurviveAnApplicationRestart() throws Exception {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.builder().start()) {
            String email = "restart-" + UUID.randomUUID() + "@example.com";

            try (ConfigurableApplicationContext first = start(postgres)) {
                first.getBean(AuthService.class).register(email, "survives a restart");
            }

            try (ConfigurableApplicationContext second = start(postgres)) {
                var login = second.getBean(AuthService.class).login(email, "survives a restart");
                assertThat(login.user().email()).isEqualTo(email);
            }
        }
    }

    private static ConfigurableApplicationContext start(EmbeddedPostgres postgres) {
        return new SpringApplicationBuilder(KairosBackendApplication.class)
            .profiles("test")
            // Command-line arguments outrank application.yml, unlike builder default properties
            .run(
                "--server.port=0",
                "--spring.datasource.url=" + postgres.getJdbcUrl("postgres", "postgres"),
                "--spring.datasource.username=postgres",
                "--spring.datasource.password=");
    }
}
