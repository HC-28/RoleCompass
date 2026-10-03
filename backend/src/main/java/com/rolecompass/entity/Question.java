package com.rolecompass.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Integer sectionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dimension_tags", columnDefinition = "text[]")
    private String[] dimensionTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_predicate", columnDefinition = "jsonb")
    private String triggerPredicate;

    /**
     * Response instrument type telling the frontend which widget to render.
     * <ul>
     *   <li>{@code LIKERT_5}    — classic 5-point Strongly Disagree to Strongly Agree (Section 1)</li>
     *   <li>{@code INTEREST_4}  — 4-point interest+exposure scale, no neutral trap (Sections 2 & 4)</li>
     *   <li>{@code PREFERENCE_4} — 4-point bipolar forced-choice between two poles (Section 3)</li>
     * </ul>
     */
    @Column(name = "response_type", length = 20)
    @Builder.Default
    private String responseType = "STYLE_5";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
