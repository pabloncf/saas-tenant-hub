package com.pabloncf.saas.common.domain;

import com.pabloncf.saas.billing.SubscriptionTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 20)
    private SubscriptionTier subscriptionTier = SubscriptionTier.FREE;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Organization() {}

    public Organization(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID             getId()                   { return id;                   }
    public String           getName()                 { return name;                 }
    public String           getSlug()                 { return slug;                 }
    public SubscriptionTier getSubscriptionTier()     { return subscriptionTier;     }
    public String           getStripeCustomerId()     { return stripeCustomerId;     }
    public String           getStripeSubscriptionId() { return stripeSubscriptionId; }
    public OffsetDateTime   getCreatedAt()            { return createdAt;            }
    public OffsetDateTime   getUpdatedAt()            { return updatedAt;            }

    public void setSubscriptionTier(SubscriptionTier tier)      { this.subscriptionTier     = tier;             }
    public void setStripeCustomerId(String stripeCustomerId)     { this.stripeCustomerId     = stripeCustomerId; }
    public void setStripeSubscriptionId(String subscriptionId)   { this.stripeSubscriptionId = subscriptionId;   }
}
