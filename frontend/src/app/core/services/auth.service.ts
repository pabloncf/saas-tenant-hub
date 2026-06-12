import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';
import { AuthUser, AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly tokens = inject(TokenStorageService);
  private readonly base   = environment.apiUrl;

  private userSubject = new BehaviorSubject<AuthUser | null>(this.decodeStored());
  user$ = this.userSubject.asObservable();

  get currentUser(): AuthUser | null { return this.userSubject.value; }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/auth/login`, req).pipe(
      tap(res => this.handleAuth(res))
    );
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/auth/register`, req).pipe(
      tap(res => this.handleAuth(res))
    );
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = this.tokens.getRefreshToken();
    return this.http.post<AuthResponse>(`${this.base}/auth/refresh`, { refreshToken }).pipe(
      tap(res => this.handleAuth(res))
    );
  }

  logout(): void {
    this.tokens.clear();
    this.userSubject.next(null);
    this.router.navigate(['/login']);
  }

  private handleAuth(res: AuthResponse): void {
    this.tokens.saveTokens(res.access_token, res.refresh_token);
    this.userSubject.next(this.decode(res.access_token));
  }

  private decodeStored(): AuthUser | null {
    const token = this.tokens.getAccessToken();
    return token ? this.decode(token) : null;
  }

  private decode(token: string): AuthUser | null {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return { userId: payload.sub, tenantId: payload.tenantId, role: payload.role };
    } catch {
      return null;
    }
  }
}
