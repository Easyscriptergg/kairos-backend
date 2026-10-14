package com.kairos.backend.training;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kairos.backend.auth.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/training-sessions")
@Tag(name = "Training sessions")
public class TrainingController {

    private final TrainingService training;

    public TrainingController(TrainingService training) {
        this.training = training;
    }

    @GetMapping
    @Operation(summary = "The athlete's sessions, oldest first, optionally within [from, to]")
    public List<TrainingSessionDto> list(
        @CurrentUser UUID userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return training.list(userId, from, to);
    }

    @PostMapping
    @Operation(summary = "Store a session. 201 when created; 200 with the stored session when this id already exists")
    public ResponseEntity<TrainingSessionDto> create(@CurrentUser UUID userId, @Valid @RequestBody TrainingSessionDto body) {
        TrainingService.CreateResult result = training.create(userId, body);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.session());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a session and its sets")
    public void delete(@CurrentUser UUID userId, @PathVariable String id) {
        training.delete(userId, id);
    }
}
