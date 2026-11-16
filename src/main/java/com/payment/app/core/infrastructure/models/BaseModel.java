package com.payment.app.core.infrastructure.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

@Getter 
@MappedSuperclass
abstract class BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @Column 
    private boolean isActive;

    @PrePersist 
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.isActive = true;
    }
}
