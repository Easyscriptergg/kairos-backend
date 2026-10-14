package com.kairos.backend.training;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reference data seeded by migration V2; mirrors the frontend exercise library. */
@Entity
@Table(name = "exercise")
public class Exercise {

    @Id
    private String id;

    private String name;

    protected Exercise() {
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
