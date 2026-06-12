import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly ACCESS_KEY  = 'access_token';
  private readonly REFRESH_KEY = 'refresh_token';

  getAccessToken(): string | null  { return localStorage.getItem(this.ACCESS_KEY);  }
  getRefreshToken(): string | null { return localStorage.getItem(this.REFRESH_KEY); }

  saveTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(this.ACCESS_KEY,  accessToken);
    localStorage.setItem(this.REFRESH_KEY, refreshToken);
  }

  clear(): void {
    localStorage.removeItem(this.ACCESS_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
  }

  isLoggedIn(): boolean { return !!this.getAccessToken(); }
}
