package com.pabloncf.saas.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pabloncf.saas.billing.SubscriptionTier;

public record BillingStatusResponse(
        SubscriptionTier tier,
        @JsonProperty("max_projects") int maxProjects,
        @JsonProperty("max_members")  int maxMembers,
        @JsonProperty("stripe_customer_id")     String stripeCustomerId,
        @JsonProperty("stripe_subscription_id") String stripeSubscriptionId
) {}
