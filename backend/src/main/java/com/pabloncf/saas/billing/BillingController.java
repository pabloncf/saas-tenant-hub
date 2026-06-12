package com.pabloncf.saas.billing;

import com.pabloncf.saas.billing.dto.BillingStatusResponse;
import com.pabloncf.saas.billing.dto.CheckoutRequest;
import com.pabloncf.saas.common.dto.ApiResponse;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final BillingService        billingService;
    private final StripeWebhookHandler  webhookHandler;

    public BillingController(BillingService billingService, StripeWebhookHandler webhookHandler) {
        this.billingService = billingService;
        this.webhookHandler = webhookHandler;
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current billing status and tier limits")
    public ResponseEntity<ApiResponse<BillingStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.of(billingService.getStatus()));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a Stripe Checkout Session for upgrading the subscription")
    public ResponseEntity<Map<String, String>> createCheckout(@Valid @RequestBody CheckoutRequest request) {
        try {
            String url = billingService.createCheckoutSession(request);
            return ResponseEntity.ok(Map.of("checkout_url", url));
        } catch (StripeException e) {
            log.error("Stripe checkout error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Billing service unavailable");
        }
    }

    @PostMapping("/portal")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a Stripe Customer Portal session for managing the subscription")
    public ResponseEntity<Map<String, String>> createPortal() {
        try {
            String url = billingService.createPortalSession();
            return ResponseEntity.ok(Map.of("portal_url", url));
        } catch (StripeException e) {
            log.error("Stripe portal error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Billing service unavailable");
        }
    }

    // No @SecurityRequirement — authenticated by Stripe signature, not JWT
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Stripe webhook receiver — authenticated via Stripe-Signature header")
    public ResponseEntity<Void> webhook(
            @RequestBody byte[] payload,
            @RequestHeader("Stripe-Signature") String signature) {
        webhookHandler.handleEvent(new String(payload, StandardCharsets.UTF_8), signature);
        return ResponseEntity.ok().build();
    }
}
