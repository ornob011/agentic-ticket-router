package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @UuidGenerator
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "uuid"
    )
    private UUID id;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "timestamptz"
    )
    private Instant createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "timestamptz"
    )
    private Instant updatedAt;

    @CreatedBy
    @Column(
            name = "created_by",
            updatable = false,
            length = 100
    )
    private String createdBy;

    @LastModifiedBy
    @Column(
            name = "updated_by",
            length = 100
    )
    private String updatedBy;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (Objects.isNull(other)) {
            return false;
        }

        if (!Objects.equals(Hibernate.getClass(this), Hibernate.getClass(other))) {
            return false;
        }

        BaseEntity that = (BaseEntity) other;
        return Objects.nonNull(getId()) && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return Objects.nonNull(getId())
                ? getId().hashCode()
                : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "BaseEntity{" +
                "id=" + id +
                ", rowVersion=" + rowVersion +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", createdBy='" + createdBy + '\'' +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
}
