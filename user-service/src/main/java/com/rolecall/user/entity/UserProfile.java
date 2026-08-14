package com.rolecall.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Id equals the owning user's id in auth-service — this row is created
 * asynchronously when {@code UserRegisteredEvent} is consumed, not via a
 * direct API call.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(length = 150)
    private String headline;

    @Column(length = 2000)
    private String bio;

    @Column(length = 30)
    private String phone;

    @Column(length = 150)
    private String location;

    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "company_website", length = 300)
    private String companyWebsite;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
