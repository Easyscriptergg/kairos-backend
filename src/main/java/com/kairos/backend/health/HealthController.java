package com.kairos.backend.health;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Public liveness check that also proves the database answers. Used by Render's health check. */
@RestController
@Tag(name = "Health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    @Operation(summary = "Service and database status")
    public ResponseEntity<Map<String, String>> health() {
        try {
            Integer one = jdbc.queryForObject("select 1", Integer.class);
            boolean up = one != null && one == 1;
            return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", up ? "UP" : "DOWN", "database", up ? "UP" : "DOWN"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status", "DOWN", "database", "DOWN"));
        }
    }
}
