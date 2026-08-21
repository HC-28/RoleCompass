package com.rolecompass.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "in_progress";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "candidate_role_ids", columnDefinition = "bigint[]")
    private Long[] candidateRoleIds;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "answered_vector", columnDefinition = "float8[]")
    private Double[] answeredVector;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "answered_dims_mask", columnDefinition = "boolean[]")
    private Boolean[] answeredDimsMask;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
