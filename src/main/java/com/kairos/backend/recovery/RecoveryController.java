package com.kairos.backend.recovery;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kairos.backend.auth.CurrentUser;
import com.kairos.backend.common.ApiException;
import com.kairos.backend.common.DateRanges;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/recovery-records")
@Tag(name = "Recovery records")
public class RecoveryController {

    private final RecoveryRecordRepository records;

    public RecoveryController(RecoveryRecordRepository records) {
        this.records = records;
    }

    @GetMapping
    @Operation(summary = "The athlete's recovery records, oldest first, optionally within [from, to]")
    @Transactional(readOnly = true)
    public List<RecoveryRecordDto> list(
        @CurrentUser UUID userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = DateRanges.fromOrEarliest(from);
        LocalDate end = DateRanges.toOrLatest(to);
        DateRanges.requireOrdered(start, end);
        return records.findForUser(userId, start, end).stream().map(RecoveryRecordDto::of).toList();
    }

    @PutMapping("/{date}")
    @Operation(summary = "Create or replace the recovery record for a day")
    @Transactional
    public RecoveryRecordDto put(
        @CurrentUser UUID userId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @Valid @RequestBody RecoveryRecordDto body) {
        if (!date.equals(body.date())) {
            throw ApiException.badRequest("The date in the path and the body must match.");
        }
        DateRanges.requirePastOrToday(date);

        RecoveryRecord record = records.findByUserIdAndRecordDate(userId, date).orElseGet(RecoveryRecord::new);
        record.userId = userId;
        record.clientId = body.id();
        record.recordDate = date;
        record.sleepHours = body.sleepDurationHours();
        record.sleepQuality = body.sleepQuality().shortValue();
        record.restingHeartRate = body.restingHeartRate().shortValue();
        record.hrvMs = body.hrvMs() == null ? null : body.hrvMs().shortValue();
        record.soreness = body.soreness().shortValue();
        record.mood = body.mood() == null ? null : body.mood().shortValue();
        record.stress = body.stress() == null ? null : body.stress().shortValue();
        record.sourceProvider = body.source().provider();
        record.recordedAt = body.source().recordedAt();
        return RecoveryRecordDto.of(records.saveAndFlush(record));
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the recovery record for a day")
    @Transactional
    public void delete(@CurrentUser UUID userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        RecoveryRecord record = records.findByUserIdAndRecordDate(userId, date)
            .orElseThrow(() -> ApiException.notFound("A recovery record for " + date));
        records.delete(record);
    }
}

@Repository
interface RecoveryRecordRepository extends JpaRepository<RecoveryRecord, UUID> {

    Optional<RecoveryRecord> findByUserIdAndRecordDate(UUID userId, LocalDate recordDate);

    @Query("select r from RecoveryRecord r where r.userId = :userId and r.recordDate between :from and :to order by r.recordDate")
    List<RecoveryRecord> findForUser(UUID userId, LocalDate from, LocalDate to);
}
