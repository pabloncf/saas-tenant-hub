package com.pabloncf.saas.billing;

import com.pabloncf.saas.config.StripeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookHandlerTest {

    private static final String TEST_SECRET = "whsec_test_secret_for_unit_testing_only";

    @Mock
    JdbcOperations adminJdbc;

    StripeWebhookHandler handler;

    @BeforeEach
    void setUp() {
        StripeConfig config = new StripeConfig();
        config.setWebhookSecret(TEST_SECRET);
        handler = new StripeWebhookHandler(adminJdbc, config);
    }

    // ── signature verification ────────────────────────────────────────────────

    @Test
    void invalid_signature_throws_400() {
        assertThatThrownBy(() -> handler.handleEvent("{}", "t=1234,v1=badhash"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void valid_signature_passes_verification() throws Exception {
        String payload   = checkoutPayload("evt_valid_001", "cus_abc", "sub_abc", "PRO");
        String signature = sign(payload, TEST_SECRET);

        when(adminJdbc.queryForObject(anyString(), eq(Boolean.class), eq("evt_valid_001")))
                .thenReturn(Boolean.FALSE);

        // No exception means signature was accepted
        handler.handleEvent(payload, signature);
    }

    // ── idempotency ───────────────────────────────────────────────────────────

    @Test
    void duplicate_event_is_skipped() throws Exception {
        String payload   = checkoutPayload("evt_dup_001", "cus_dup", "sub_dup", "PRO");
        String signature = sign(payload, TEST_SECRET);

        when(adminJdbc.queryForObject(anyString(), eq(Boolean.class), eq("evt_dup_001")))
                .thenReturn(Boolean.TRUE);

        handler.handleEvent(payload, signature);

        verify(adminJdbc, never()).update(contains("UPDATE organizations"), anyString(), anyString(), anyString());
        verify(adminJdbc, never()).update(contains("INSERT INTO stripe_events"), anyString(), anyString());
    }

    // ── business logic (tested directly via package-private methods) ─────────

    @Test
    void handleCheckoutCompleted_updates_tier_and_subscription() {
        handler.handleCheckoutCompleted("cus_123", "sub_456", "PRO");

        verify(adminJdbc).update(
                "UPDATE organizations SET subscription_tier = ?, stripe_subscription_id = ? WHERE stripe_customer_id = ?",
                "PRO", "sub_456", "cus_123"
        );
    }

    @Test
    void handleInvoicePaymentFailed_downgrades_to_free() {
        handler.handleInvoicePaymentFailed("cus_failed_123");

        verify(adminJdbc).update(
                "UPDATE organizations SET subscription_tier = 'FREE' WHERE stripe_customer_id = ?",
                "cus_failed_123"
        );
    }

    @Test
    void handleSubscriptionDeleted_downgrades_and_clears_subscription_id() {
        handler.handleSubscriptionDeleted("cus_cancelled_123");

        verify(adminJdbc).update(
                "UPDATE organizations SET subscription_tier = 'FREE', stripe_subscription_id = NULL WHERE stripe_customer_id = ?",
                "cus_cancelled_123"
        );
    }

    @Test
    void handleCheckoutCompleted_with_null_tier_skips_update() {
        handler.handleCheckoutCompleted("cus_no_tier", "sub_no_tier", null);

        verify(adminJdbc, never()).update(contains("UPDATE organizations"), anyString(), anyString(), anyString());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String checkoutPayload(String eventId, String customerId,
                                          String subscriptionId, String tier) {
        return """
                {
                  "id": "%s",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "api_version": "2024-04-10",
                  "created": 1700000000,
                  "livemode": false,
                  "pending_webhooks": 1,
                  "request": null,
                  "data": {
                    "object": {
                      "id": "cs_test_001",
                      "object": "checkout.session",
                      "customer": "%s",
                      "subscription": "%s",
                      "payment_status": "paid",
                      "status": "complete",
                      "mode": "subscription",
                      "metadata": { "tier": "%s", "organizationId": "00000000-0000-0000-0000-000000000001" }
                    }
                  }
                }
                """.formatted(eventId, customerId, subscriptionId, tier);
    }

    private static String sign(String payload, String secret) throws Exception {
        long timestamp    = System.currentTimeMillis() / 1000;
        String signed     = timestamp + "." + payload;
        Mac mac           = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash       = mac.doFinal(signed.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb  = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return "t=" + timestamp + ",v1=" + sb;
    }
}
