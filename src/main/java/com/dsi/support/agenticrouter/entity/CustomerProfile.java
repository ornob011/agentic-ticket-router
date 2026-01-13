package com.dsi.support.agenticrouter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(
        name = "customer_profile",
        indexes = {
                @Index(
                        name = "idx_customer_profile_user_id",
                        columnList = "user_id",
                        unique = true
                ),
                @Index(
                        name = "idx_customer_profile_country",
                        columnList = "country_iso2"
                ),
                @Index(
                        name = "idx_customer_profile_tier",
                        columnList = "tier_code"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile extends BaseEntity {

    @NotNull(message = "User is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_customer_profile_user")
    )
    private AppUser user;

    @Size(max = 100)
    @Column(
            name = "company_name",
            length = 100
    )
    private String companyName;

    @Size(max = 20)
    @Column(
            name = "phone_number",
            length = 20
    )
    private String phoneNumber;

    @Size(max = 255)
    @Column(name = "address")
    private String address;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "country_iso2",
            foreignKey = @ForeignKey(name = "fk_customer_profile_country")
    )
    private Country country;

    @Size(max = 20)
    @Column(
            name = "postal_code",
            length = 20
    )
    private String postalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "tier_code",
            foreignKey = @ForeignKey(name = "fk_customer_profile_tier")
    )
    private CustomerTier customerTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "preferred_language_code",
            foreignKey = @ForeignKey(name = "fk_customer_profile_language")
    )
    private Language preferredLanguage;

    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private boolean notificationsEnabled = true;

    @Override
    public String toString() {
        return "CustomerProfile{" +
                "id=" + getId() +
                ", userId=" + (user != null ? user.getId() : null) +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}
