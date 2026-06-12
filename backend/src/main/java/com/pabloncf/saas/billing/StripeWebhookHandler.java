package com.pabloncf.saas.billing;

import com.pabloncf.saas.config.StripeConfig;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.Optional;

@Service
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);

    private final JdbcOperations adminJdbc;
    private final StripeConfig   stripeConfig;

    // Production constructor — Spring uses adminDataSource (saas_user, bypasses RLS)
    public StripeWebhookHandler(
            @Qualifier("adminDataSource") DataSource adminDataSource,
            StripeConfig stripeConfig) {
        this.adminJdbc    = new JdbcTemplate(adminDataSource);
        this.stripeConfig = stripeConfig;
    }

    // Package-private constructor for unit tests
    StripeWebhookHandler(JdbcOperations adminJdbc, StripeConfig stripeConfig) {
        this.adminJdbc    = adminJdbc;
        this.stripeConfig = stripeConfig;
    }

    public void handleEvent(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }

        if (isAlreadyProcessed(event.getId())) {
            log.debug("Skipping duplicate Stripe event {}", event.getId());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed"    -> handleCheckoutCompleted(event);
            case "invoice.paid"                  -> handleInvoicePaid(event);
            case "invoice.payment_failed"        -> handleInvoicePaymentFailed(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default                              -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        markAsProcessed(event.getId(), event.getType());
    }

    // ── event-specific handlers (package-private for direct testing) ──────────

    void handleCheckoutCompleted(Event event) {
        deserialize(event, Session.class).ifPresentOrElse(
                session -> handleCheckoutCompleted(
                        session.getCustomer(),
                        session.getSubscription(),
                        session.getMetadata().get("tier")),
                () -> log.warn("Could not deserialize checkout.session.completed event {}", event.getId())
        );
    }

    void handleCheckoutCompleted(String customerId, String subscriptionId, String tier) {
        if (tier == null) {
            log.warn("Missing tier metadata in checkout session for customer {}", customerId);
            return;
        }
        int updated = adminJdbc.update(
                "UPDATE organizations SET subscription_tier = ?, stripe_subscription_id = ? WHERE stripe_customer_id = ?",
                tier, subscriptionId, customerId
        );
        log.info("Activated {} tier for customer {} (rows updated: {})", tier, customerId, updated);
    }

    void handleInvoicePaid(Event event) {
        deserialize(event, Invoice.class).ifPresent(invoice ->
                log.info("Invoice paid for customer {}", invoice.getCustomer())
        );
    }

    void handleInvoicePaymentFailed(Event event) {
        deserialize(event, Invoice.class).ifPresentOrElse(
                invoice -> handleInvoicePaymentFailed(invoice.getCustomer()),
                () -> log.warn("Could not deserialize invoice.payment_failed event {}", event.getId())
        );
    }

    void handleInvoicePaymentFailed(String customerId) {
        int updated = adminJdbc.update(
                "UPDATE organizations SET subscription_tier = 'FREE' WHERE stripe_customer_id = ?",
                customerId
        );
        log.warn("Payment failed — downgraded customer {} to FREE (rows updated: {})", customerId, updated);
    }

    void handleSubscriptionDeleted(Event event) {
        deserialize(event, Subscription.class).ifPresentOrElse(
                sub -> handleSubscriptionDeleted(sub.getCustomer()),
                () -> log.warn("Could not deserialize customer.subscription.deleted event {}", event.getId())
        );
    }

    void handleSubscriptionDeleted(String customerId) {
        int updated = adminJdbc.update(
                "UPDATE organizations SET subscription_tier = 'FREE', stripe_subscription_id = NULL WHERE stripe_customer_id = ?",
                customerId
        );
        log.info("Subscription cancelled — downgraded customer {} to FREE (rows updated: {})", customerId, updated);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private <T extends StripeObject> Optional<T> deserialize(Event event, Class<T> type) {
        return event.getDataObjectDeserializer()
                .getObject()
                .filter(type::isInstance)
                .map(type::cast);
    }

    private boolean isAlreadyProcessed(String eventId) {
        Boolean exists = adminJdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM stripe_events WHERE id = ?)",
                Boolean.class, eventId
        );
        return Boolean.TRUE.equals(exists);
    }

    private void markAsProcessed(String eventId, String eventType) {
        adminJdbc.update(
                "INSERT INTO stripe_events (id, type) VALUES (?, ?)",
                eventId, eventType
        );
    }
}
