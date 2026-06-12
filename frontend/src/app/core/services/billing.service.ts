import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { BillingStatus, CheckoutRequest } from '../models/billing.model';

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/billing`;

  getStatus(): Observable<ApiResponse<BillingStatus>> {
    return this.http.get<ApiResponse<BillingStatus>>(`${this.base}/status`);
  }

  createCheckout(req: CheckoutRequest): Observable<{ checkout_url: string }> {
    return this.http.post<{ checkout_url: string }>(`${this.base}/checkout`, req);
  }

  createPortal(): Observable<{ portal_url: string }> {
    return this.http.post<{ portal_url: string }>(`${this.base}/portal`, {});
  }
}
