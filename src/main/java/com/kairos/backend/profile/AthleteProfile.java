package com.kairos.backend.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "athlete_profile")
public class AthleteProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    private String name;
    private String sport;
    private String discipline;
    private String experience;
    private String goal;

    @Column(name = "training_frequency")
    private short trainingFrequency;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> equipment = new ArrayList<>();

    @Column(name = "performance_level")
    private String performanceLevel;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    private Short age;

    @Column(name = "created_on")
    private LocalDate createdOn;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected AthleteProfile() {
    }

    public AthleteProfile(UUID userId, LocalDate createdOn) {
        this.userId = userId;
        this.createdOn = createdOn;
    }

    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public String getDiscipline() { return discipline; }
    public String getExperience() { return experience; }
    public String getGoal() { return goal; }
    public short getTrainingFrequency() { return trainingFrequency; }
    public List<String> getEquipment() { return equipment; }
    public String getPerformanceLevel() { return performanceLevel; }
    public Double getHeightCm() { return heightCm; }
    public Double getWeightKg() { return weightKg; }
    public Short getAge() { return age; }
    public LocalDate getCreatedOn() { return createdOn; }

    void update(ProfileDto dto) {
        this.name = dto.name().trim();
        this.sport = dto.sport();
        this.discipline = dto.discipline().trim();
        this.experience = dto.experience();
        this.goal = dto.goal() == null ? "" : dto.goal().trim();
        this.trainingFrequency = dto.trainingFrequency().shortValue();
        this.equipment = new ArrayList<>(dto.equipment());
        this.performanceLevel = dto.performanceLevel();
        ProfileDto.BodyMetrics body = dto.bodyMetrics();
        this.heightCm = body == null ? null : body.height();
        this.weightKg = body == null ? null : body.weight();
        this.age = body == null || body.age() == null ? null : body.age().shortValue();
        this.updatedAt = Instant.now();
    }
}
