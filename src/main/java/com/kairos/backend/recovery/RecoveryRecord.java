package com.kairos.backend.recovery;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One morning check-in per athlete per calendar day. The recovery score is calculated by the client, never stored. */
@Entity
@Table(name = "recovery_record")
public class RecoveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "client_id", nullable = false)
    String clientId;

    @Column(name = "record_date", nullable = false)
    LocalDate recordDate;

    @Column(name = "sleep_hours")
    double sleepHours;

    @Column(name = "sleep_quality")
    short sleepQuality;

    @Column(name = "resting_heart_rate")
    short restingHeartRate;

    @Column(name = "hrv_ms")
    Short hrvMs;

    short soreness;
    Short mood;
    Short stress;

    @Column(name = "source_provider")
    String sourceProvider;

    @Column(name = "recorded_at")
    Instant recordedAt;
}
