import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MemberService } from '../core/services/member.service';
import { AuthService } from '../core/services/auth.service';
import { Member } from '../core/models/member.model';
import { Role } from '../core/models/auth.model';

@Component({
  selector: 'app-members',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './members.component.html',
  styleUrl: './members.component.scss'
})
export class MembersComponent implements OnInit {
  private memberService = inject(MemberService);
  protected auth        = inject(AuthService);

  members: Member[] = [];
  loading = true;
  error   = '';

  readonly roles: Role[] = ['VIEWER', 'MEMBER', 'ADMIN', 'OWNER'];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.memberService.list().subscribe({
      next: res => { this.members = res.data; this.loading = false; },
      error: () => { this.error = 'Failed to load members'; this.loading = false; }
    });
  }

  updateRole(member: Member, role: Role): void {
    if (role === member.role) return;
    this.memberService.updateRole(member.user_id, { role }).subscribe({
      next: res => { member.role = res.data.role; },
      error: err => alert(err.error?.message ?? 'Failed to update role')
    });
  }

  remove(member: Member): void {
    if (!confirm(`Remove ${member.full_name} from the organization?`)) return;
    this.memberService.remove(member.user_id).subscribe({
      next: () => this.load(),
      error: err => alert(err.error?.message ?? 'Failed to remove member')
    });
  }

  isCurrentUser(member: Member): boolean {
    return member.user_id === this.auth.currentUser?.userId;
  }

  canRemove(member: Member): boolean {
    const myRole = this.auth.currentUser?.role;
    return myRole === 'OWNER' && !this.isCurrentUser(member) && member.role !== 'OWNER';
  }

  canChangeRole(member: Member): boolean {
    const myRole = this.auth.currentUser?.role;
    return (myRole === 'OWNER' || myRole === 'ADMIN')
        && !this.isCurrentUser(member)
        && member.role !== 'OWNER';
  }
}
