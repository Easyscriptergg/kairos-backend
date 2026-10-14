-- Kairos core schema.
-- Records store observations only. Scores, loads, totals and insights are
-- calculated from them and are never stored, so a formula change applies to
-- all history at once.
--
-- Every athlete-owned row carries user_id, and every lookup is scoped by it.
-- `client_id` is the identifier the frontend generates; it is unique per
-- user, which makes repeated creates idempotent.

CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX app_user_email_uq ON app_user (lower(email));

CREATE TABLE athlete_profile (
    user_id             UUID PRIMARY KEY REFERENCES app_user (id) ON DELETE CASCADE,
    name                VARCHAR(120) NOT NULL,
    sport               VARCHAR(32)  NOT NULL,
    discipline          VARCHAR(120) NOT NULL,
    experience          VARCHAR(32)  NOT NULL,
    goal                VARCHAR(500) NOT NULL DEFAULT '',
    training_frequency  SMALLINT     NOT NULL CHECK (training_frequency BETWEEN 1 AND 14),
    equipment           TEXT[]       NOT NULL DEFAULT '{}',
    performance_level   VARCHAR(32)  NOT NULL,
    height_cm           DOUBLE PRECISION CHECK (height_cm > 0 AND height_cm < 300),
    weight_kg           DOUBLE PRECISION CHECK (weight_kg > 0 AND weight_kg < 500),
    age                 SMALLINT     CHECK (age > 0 AND age < 120),
    created_on          DATE         NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Reference data: exercises a session may refer to (seeded in V2)
CREATE TABLE exercise (
    id   VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL
);

CREATE TABLE training_session (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    client_id        VARCHAR(80)  NOT NULL,
    session_date     DATE         NOT NULL,
    name             VARCHAR(120) NOT NULL,
    type             VARCHAR(32)  NOT NULL,
    duration_minutes INTEGER      NOT NULL CHECK (duration_minutes BETWEEN 1 AND 1440),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT training_session_client_uq UNIQUE (user_id, client_id)
);
CREATE INDEX training_session_user_date_idx ON training_session (user_id, session_date);

CREATE TABLE training_exercise (
    id          UUID PRIMARY KEY,
    session_id  UUID        NOT NULL REFERENCES training_session (id) ON DELETE CASCADE,
    client_id   VARCHAR(80) NOT NULL,
    exercise_id VARCHAR(64) NOT NULL REFERENCES exercise (id),
    position    SMALLINT    NOT NULL
);
CREATE INDEX training_exercise_session_idx ON training_exercise (session_id);

CREATE TABLE training_set (
    id                   UUID PRIMARY KEY,
    training_exercise_id UUID             NOT NULL REFERENCES training_exercise (id) ON DELETE CASCADE,
    client_id            VARCHAR(80)      NOT NULL,
    set_number           SMALLINT         NOT NULL CHECK (set_number >= 1),
    weight_kg            DOUBLE PRECISION NOT NULL CHECK (weight_kg >= 0 AND weight_kg <= 1000),
    reps                 SMALLINT         NOT NULL CHECK (reps >= 1),
    rpe                  DOUBLE PRECISION CHECK (rpe BETWEEN 1 AND 10),
    rest_seconds         INTEGER          CHECK (rest_seconds >= 0),
    performed_at         TIMESTAMPTZ      NOT NULL
);
CREATE INDEX training_set_exercise_idx ON training_set (training_exercise_id);

-- One recovery record per athlete per calendar day
CREATE TABLE recovery_record (
    id                   UUID PRIMARY KEY,
    user_id              UUID             NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    client_id            VARCHAR(80)      NOT NULL,
    record_date          DATE             NOT NULL,
    sleep_hours          DOUBLE PRECISION NOT NULL CHECK (sleep_hours > 0 AND sleep_hours <= 24),
    sleep_quality        SMALLINT         NOT NULL CHECK (sleep_quality BETWEEN 0 AND 100),
    resting_heart_rate   SMALLINT         NOT NULL CHECK (resting_heart_rate BETWEEN 25 AND 150),
    hrv_ms               SMALLINT         CHECK (hrv_ms BETWEEN 1 AND 300),
    soreness             SMALLINT         NOT NULL CHECK (soreness BETWEEN 1 AND 10),
    mood                 SMALLINT         CHECK (mood BETWEEN 1 AND 10),
    stress               SMALLINT         CHECK (stress BETWEEN 1 AND 10),
    source_provider      VARCHAR(40)      NOT NULL,
    recorded_at          TIMESTAMPTZ      NOT NULL,
    CONSTRAINT recovery_record_day_uq UNIQUE (user_id, record_date)
);

-- One nutrition day per athlete per calendar day; totals are the sum of its meals
CREATE TABLE nutrition_day (
    id                  UUID PRIMARY KEY,
    user_id             UUID             NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    client_id           VARCHAR(80)      NOT NULL,
    day_date            DATE             NOT NULL,
    hydration_liters    DOUBLE PRECISION CHECK (hydration_liters >= 0 AND hydration_liters <= 20),
    target_calories     INTEGER          NOT NULL CHECK (target_calories > 0),
    target_protein      INTEGER          NOT NULL CHECK (target_protein >= 0),
    target_carbohydrates INTEGER         NOT NULL CHECK (target_carbohydrates >= 0),
    target_fat          INTEGER          NOT NULL CHECK (target_fat >= 0),
    target_hydration    DOUBLE PRECISION NOT NULL CHECK (target_hydration >= 0),
    CONSTRAINT nutrition_day_uq UNIQUE (user_id, day_date)
);

CREATE TABLE meal (
    id               UUID PRIMARY KEY,
    nutrition_day_id UUID         NOT NULL REFERENCES nutrition_day (id) ON DELETE CASCADE,
    client_id        VARCHAR(80)  NOT NULL,
    name             VARCHAR(120) NOT NULL,
    meal_time        VARCHAR(5)   NOT NULL CHECK (meal_time ~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'),
    calories         INTEGER      NOT NULL CHECK (calories >= 0),
    protein          INTEGER      NOT NULL CHECK (protein >= 0),
    carbohydrates    INTEGER      NOT NULL CHECK (carbohydrates >= 0),
    fat              INTEGER      NOT NULL CHECK (fat >= 0),
    position         SMALLINT     NOT NULL
);
CREATE INDEX meal_day_idx ON meal (nutrition_day_id);
