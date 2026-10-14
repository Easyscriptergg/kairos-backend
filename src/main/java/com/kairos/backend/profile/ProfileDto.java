package com.kairos.backend.profile;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Matches the frontend's AthleteProfile type. {@code id} and {@code createdAt} are set by the server. */
public record ProfileDto(
    String id,
    @NotBlank @Size(max = 120) String name,
    @NotNull @Pattern(regexp = "strength|running|cycling|team-sport|combat|hybrid") String sport,
    @NotBlank @Size(max = 120) String discipline,
    @NotNull @Pattern(regexp = "beginner|intermediate|advanced|elite") String experience,
    @Size(max = 500) String goal,
    @NotNull @Min(1) @Max(14) Integer trainingFrequency,
    @NotNull @Size(max = 30) List<@NotBlank @Size(max = 40) String> equipment,
    @NotNull @Pattern(regexp = "recreational|competitive|semi-pro|professional") String performanceLevel,
    @Valid BodyMetrics bodyMetrics,
    String createdAt,
    Boolean onboardingComplete) {

    public record BodyMetrics(
        @DecimalMin(value = "0", inclusive = false) @DecimalMax(value = "300", inclusive = false) Double height,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax(value = "500", inclusive = false) Double weight,
        @Min(1) @Max(119) Integer age) {
    }

    static ProfileDto of(AthleteProfile p) {
        return new ProfileDto(
            p.getUserId().toString(),
            p.getName(),
            p.getSport(),
            p.getDiscipline(),
            p.getExperience(),
            p.getGoal(),
            (int) p.getTrainingFrequency(),
            List.copyOf(p.getEquipment()),
            p.getPerformanceLevel(),
            new BodyMetrics(p.getHeightCm(), p.getWeightKg(), p.getAge() == null ? null : p.getAge().intValue()),
            p.getCreatedOn().toString(),
            true);
    }
}
