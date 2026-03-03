package com.dsi.support.agenticrouter.entity;

import com.dsi.support.agenticrouter.enums.NextAction;
import com.dsi.support.agenticrouter.enums.TicketCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "routing_pattern",
    indexes = {
        @Index(name = "idx_pattern_category", columnList = "category"),
        @Index(name = "idx_pattern_action", columnList = "successful_action"),
        @Index(name = "idx_pattern_success", columnList = "success_count")
    }
)
public class RoutingPattern extends BaseEntity {

    private static final int RELIABILITY_MIN_SAMPLES = 5;
    private static final double RELIABILITY_MIN_SUCCESS_RATE = 0.7;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private TicketCategory category;

    @ElementCollection
    @CollectionTable(
        name = "routing_pattern_keyword",
        joinColumns = @JoinColumn(name = "routing_pattern_id")
    )
    @Column(name = "keyword", nullable = false, length = 200)
    private List<String> keywords;

    @Enumerated(EnumType.STRING)
    @Column(name = "successful_action", nullable = false, length = 50)
    private NextAction successfulAction;

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Integer successCount = 1;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Integer failureCount = 0;

    @Column(name = "last_success_at", columnDefinition = "timestamptz")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at", columnDefinition = "timestamptz")
    private Instant lastFailureAt;

    @Column(name = "confidence_boost", nullable = false)
    @Builder.Default
    private Double confidenceBoost = 0.1;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    public void recordSuccess() {
        this.successCount = safeIncrement(
            successCount
        );

        this.lastSuccessAt = Instant.now();
    }

    public void recordFailure() {
        this.failureCount = safeIncrement(
            failureCount
        );

        this.lastFailureAt = Instant.now();
    }

    public double getSuccessRate() {
        int total = getTotalAttempts();

        return total == 0
            ? 0.0
            : (double) getSafeCount(successCount) / total;
    }

    public boolean isReliable() {
        int total = getTotalAttempts();

        return total >= RELIABILITY_MIN_SAMPLES
               && getSuccessRate() >= RELIABILITY_MIN_SUCCESS_RATE;
    }

    private int getTotalAttempts() {
        return getSafeCount(successCount)
               + getSafeCount(failureCount);
    }

    private static int getSafeCount(
        Integer value
    ) {
        return Objects.nonNull(value)
            ? value
            : 0;
    }

    private static Integer safeIncrement(
        Integer value
    ) {
        return getSafeCount(value) + 1;
    }
}
