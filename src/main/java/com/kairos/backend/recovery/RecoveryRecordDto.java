package com.kairos.backend.recovery;

import java.time.Instant;
import java.time.LocalDate;

import com.kairos.backend.common.ClientIds;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Wire shape of a recovery record (frontend: RecoveryRecordDto in src/data/api/contract.ts). */
public record RecoveryRecordDto(
    @NotNull @Pattern(regexp = ClientIds.PATTERN, message = ClientIds.MESSAGE) String id,
    @NotNull LocalDate date,
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("24") Double sleepDurationHours,
    @NotNull @Min(0) @Max(100) Integer sleepQuality,
    @NotNull @Min(25) @Max(150) Integer restingHeartRate,
    @Min(1) @Max(300) Integer hrvMs,
    @NotNull @Min(1) @Max(10) Integer soreness,
    @Min(1) @Max(10) Integer mood,
    @Min(1) @Max(10) Integer stress,
    @NotNull @Valid Source source) {

    /**
     * Provenance of the observation. Only {@code manual} is accepted: records from a wearable must arrive
     * through a provider integration, which does not exist yet, never through this endpoint.
     */
    public record Source(
        @NotNull @Pattern(regexp = "manual", message = "only manual check-ins are accepted until a provider integration exists") String provider,
        @NotNull Instant recordedAt) {
    }

    static RecoveryRecordDto of(RecoveryRecord r) {
        return new RecoveryRecordDto(
            r.clientId,
            r.recordDate,
            r.sleepHours,
            (int) r.sleepQuality,
            (int) r.restingHeartRate,
            r.hrvMs == null ? null : r.hrvMs.intValue(),
            (int) r.soreness,
            r.mood == null ? null : r.mood.intValue(),
            r.stress == null ? null : r.stress.intValue(),
            new Source(r.sourceProvider, r.recordedAt));
    }
}
