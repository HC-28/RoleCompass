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

    /**
     * Current FSM state. Persisted so every answer submission can resume correctly.
     * Maps to {@link com.rolecompass.routing.FsmState} enum name.
     */
    @Column(name = "fsm_state", nullable = false)
    @Builder.Default
    private String fsmState = "SECTION_1_RIASEC";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "candidate_role_ids", columnDefinition = "bigint[]")
    private Long[] candidateRoleIds;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "answered_vector", columnDefinition = "float8[]")
    private Double[] answeredVector;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "answered_dims_mask", columnDefinition = "boolean[]")
    private Boolean[] answeredDimsMask;

    @Column(name = "predicted_role")
    private String predictedRole;

    @Column(name = "confidence")
    private Double confidence;

    /**
     * JSON array of EliminatedRole records accumulated during routing gate passes.
     * Serialised/deserialised by SessionService using Jackson ObjectMapper.
     * Example: [{"role":"Data Scientist","stage":"PSYCHOMETRIC","reason":"..."}]
     */
    @Column(name = "elimination_log_json", columnDefinition = "TEXT")
    private String eliminationLogJson;

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
