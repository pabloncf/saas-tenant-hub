export type SubscriptionTier = 'FREE' | 'PRO' | 'ENTERPRISE';

export interface BillingStatus {
  tier: SubscriptionTier;
  max_projects: number;
  max_members: number;
  stripe_customer_id?: string;
  stripe_subscription_id?: string;
}

export interface CheckoutRequest {
  tier: SubscriptionTier;
}
