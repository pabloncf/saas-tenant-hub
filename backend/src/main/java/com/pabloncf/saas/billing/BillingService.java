package com.pabloncf.saas.billing;

import com.pabloncf.saas.billing.dto.BillingStatusResponse;
import com.pabloncf.saas.billing.dto.CheckoutRequest;
import com.pabloncf.saas.common.domain.Organization;
import com.pabloncf.saas.common.domain.OrganizationRepository;
import com.pabloncf.saas.config.StripeConfig;
import com.pabloncf.saas.tenant.TenantContext;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class BillingService {

    private final OrganizationRepository orgRepository;
    private final StripeConfig           stripeConfig;

    @Value("${app.base-url}")
    private String baseUrl;

    public BillingService(OrganizationRepository orgRepository, StripeConfig stripeConfig) {
        this.orgRepository = orgRepository;
        this.stripeConfig  = stripeConfig;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = stripeConfig.getSecretKey();
    }

    public BillingStatusResponse getStatus() {
        Organization org = currentOrg();
        SubscriptionTier tier = org.getSubscriptionTier();
        return new BillingStatusResponse(
                tier,
                tier.getMaxProjects(),
                tier.getMaxMembers(),
                org.getStripeCustomerId(),
                org.getStripeSubscriptionId()
        );
    }

    @Transactional
    public String createCheckoutSession(CheckoutRequest request) throws StripeException {
        if (request.tier() == SubscriptionTier.FREE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot checkout for FREE tier");
        }

        Organization org = currentOrg();

        if (org.getStripeCustomerId() == null) {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .putMetadata("organizationId", org.getId().toString())
                    .build();
            Customer customer = Customer.create(customerParams);
            org.setStripeCustomerId(customer.getId());
            orgRepository.save(org);
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(org.getStripeCustomerId())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId(request.tier()))
                        .setQuantity(1L)
                        .build())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("tier", request.tier().name())
                        .putMetadata("organizationId", org.getId().toString())
                        .build())
                .setSuccessUrl(baseUrl + "/billing/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/billing")
                .putMetadata("tier", request.tier().name())
                .putMetadata("organizationId", org.getId().toString())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public String createPortalSession() throws StripeException {
        Organization org = currentOrg();

        if (org.getStripeCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No Stripe customer found — complete a checkout first");
        }

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(org.getStripeCustomerId())
                        .setReturnUrl(baseUrl + "/billing")
                        .build();

        com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(params);
        return session.getUrl();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Organization currentOrg() {
        UUID tenantId = TenantContext.getCurrentTenant();
        return orgRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Organization not found"));
    }

    private String priceId(SubscriptionTier tier) {
        return switch (tier) {
            case PRO        -> stripeConfig.getPriceIds().getPro();
            case ENTERPRISE -> stripeConfig.getPriceIds().getEnterprise();
            case FREE       -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                       "No price for FREE tier");
        };
    }
}
