package com.pabloncf.saas.billing.dto;

import com.pabloncf.saas.billing.SubscriptionTier;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "tier is required")
        SubscriptionTier tier
) {}
