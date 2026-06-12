import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProjectListComponent } from './projects/project-list/project-list.component';
import { ProjectFormComponent } from './projects/project-form/project-form.component';
import { MembersComponent } from './members/members.component';
import { BillingComponent } from './billing/billing.component';

export const routes: Routes = [
  { path: '',        redirectTo: 'login', pathMatch: 'full' },
  { path: 'login',    component: LoginComponent    },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard',           component: DashboardComponent   },
      { path: 'projects',            component: ProjectListComponent  },
      { path: 'projects/new',        component: ProjectFormComponent  },
      { path: 'projects/:id/edit',   component: ProjectFormComponent  },
      { path: 'members',             component: MembersComponent      },
      { path: 'billing',             component: BillingComponent      },
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
