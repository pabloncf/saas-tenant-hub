import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { throwError, catchError, switchMap } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokens = inject(TokenStorageService);
  const auth   = inject(AuthService);
  const router = inject(Router);

  const addBearer = (r: typeof req) => {
    const token = tokens.getAccessToken();
    return token ? r.clone({ headers: r.headers.set('Authorization', `Bearer ${token}`) }) : r;
  };

  return next(addBearer(req)).pipe(
    catchError(err => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401) {
        return throwError(() => err);
      }

      // Never retry auth endpoints — prevents infinite loops
      if (req.url.includes('/auth/')) {
        auth.logout();
        return throwError(() => err);
      }

      return auth.refresh().pipe(
        switchMap(() => next(addBearer(req))),
        catchError(refreshErr => {
          auth.logout();
          router.navigate(['/login']);
          return throwError(() => refreshErr);
        })
      );
    })
  );
};
