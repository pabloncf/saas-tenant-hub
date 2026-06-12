import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Member, UpdateRoleRequest } from '../models/member.model';

@Injectable({ providedIn: 'root' })
export class MemberService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/members`;

  list(page = 0, size = 20): Observable<ApiResponse<Member[]>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<Member[]>>(this.base, { params });
  }

  updateRole(userId: string, req: UpdateRoleRequest): Observable<ApiResponse<Member>> {
    return this.http.put<ApiResponse<Member>>(`${this.base}/${userId}/role`, req);
  }

  remove(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${userId}`);
  }
}
