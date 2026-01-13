package com.dsi.support.agenticrouter.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "model_registry",
        indexes = {
                @Index(
                        name = "idx_model_registry_model_tag",
                        columnList = "model_tag",
                        unique = true
                ),
                @Index(
                        name = "idx_model_registry_active",
                        columnList = "active"
                ),
                @Index(
                        name = "idx_model_registry_active_default",
                        columnList = "active, is_default"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelRegistry extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(
            name = "model_tag",
            nullable = false,
            unique = true,
            length = 100
    )
    private String modelTag;

    @NotBlank
    @Size(max = 200)
    @Column(
            name = "model_name",
            nullable = false,
            length = 200
    )
    private String modelName;

    @NotBlank
    @Size(max = 50)
    @Column(
            name = "version",
            nullable = false,
            length = 50
    )
    private String version;

    @Size(max = 1000)
    @Column(
            name = "description",
            columnDefinition = "text"
    )
    private String description;

    @Size(max = 50)
    @Column(name = "base_model", length = 50)
    private String baseModel;

    @Size(max = 50)
    @Column(name = "training_method", length = 50)
    private String trainingMethod;

    @Size(max = 50)
    @Column(name = "quantization", length = 50)
    private String quantization;

    @Size(max = 500)
    @Column(name = "artifact_path", length = 500)
    private String artifactPath;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = false;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(
            name = "activated_at",
            columnDefinition = "timestamptz"
    )
    private Instant activatedAt;

    @Column(
            name = "deactivated_at",
            columnDefinition = "timestamptz"
    )
    private Instant deactivatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "activated_by_id",
            foreignKey = @ForeignKey(name = "fk_model_registry_activated_by")
    )
    private AppUser activatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "performance_metrics",
            columnDefinition = "jsonb"
    )
    private JsonNode performanceMetrics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "model_config",
            columnDefinition = "jsonb"
    )
    private JsonNode modelConfig;

    public void activate(AppUser activator) {
        active = true;
        activatedAt = Instant.now();
        activatedBy = activator;
        deactivatedAt = null;
    }

    public void deactivate() {
        active = false;
        deactivatedAt = Instant.now();
    }

    public void setAsDefault(AppUser activator) {
        isDefault = true;
        if (!active) {
            activate(activator);
        }
    }

    @Override
    public String toString() {
        return "ModelRegistry{" +
                "id=" + getId() +
                ", modelTag='" + modelTag + '\'' +
                ", version='" + version + '\'' +
                ", active=" + active +
                ", isDefault=" + isDefault +
                '}';
    }
}
