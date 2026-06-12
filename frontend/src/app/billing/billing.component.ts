import { Component, OnInit, inject } from '@angular/core';
import { BillingService } from '../core/services/billing.service';
import { BillingStatus, SubscriptionTier } from '../core/models/billing.model';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [],
  templateUrl: './billing.component.html',
  styleUrl: './billing.component.scss'
})
export class BillingComponent implements OnInit {
  private billingService = inject(BillingService);

  status: BillingStatus | null = null;
  loading        = true;
  checkoutLoading: SubscriptionTier | null = null;
  portalLoading  = false;
  error          = '';

  ngOnInit(): void {
    this.billingService.getStatus().subscribe({
      next: res => { this.status = res.data; this.loading = false; },
      error: () => { this.error = 'Failed to load billing info'; this.loading = false; }
    });
  }

  upgrade(tier: SubscriptionTier): void {
    this.checkoutLoading = tier;
    this.billingService.createCheckout({ tier }).subscribe({
      next: res => { window.location.href = res.checkout_url; },
      error: err => {
        this.error          = err.error?.message ?? 'Checkout unavailable';
        this.checkoutLoading = null;
      }
    });
  }

  openPortal(): void {
    this.portalLoading = true;
    this.billingService.createPortal().subscribe({
      next: res => { window.location.href = res.portal_url; },
      error: err => {
        this.error         = err.error?.message ?? 'Portal unavailable';
        this.portalLoading = false;
      }
    });
  }
}
