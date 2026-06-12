import { Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../core/services/project.service';
import { Project, ProjectStatus } from '../../core/models/project.model';
import { PageMeta } from '../../core/models/api-response.model';

@Component({
  selector: 'app-project-list',
  standalone: true,
  imports: [RouterLink, FormsModule, DatePipe],
  templateUrl: './project-list.component.html',
  styleUrl: './project-list.component.scss'
})
export class ProjectListComponent implements OnInit {
  private projectService = inject(ProjectService);

  projects: Project[] = [];
  meta: PageMeta | null = null;
  loading   = true;
  error     = '';
  page      = 0;
  pageSize  = 20;
  filterStatus: ProjectStatus | '' = '';
  search    = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.projectService.list(
      this.page,
      this.pageSize,
      this.filterStatus || undefined,
      this.search || undefined
    ).subscribe({
      next: res => {
        this.projects = res.data;
        this.meta     = res.meta ?? null;
        this.loading  = false;
      },
      error: () => {
        this.error   = 'Failed to load projects';
        this.loading = false;
      }
    });
  }

  delete(project: Project): void {
    if (!confirm(`Delete "${project.name}"?`)) return;
    this.projectService.delete(project.id).subscribe({
      next: () => this.load(),
      error: () => alert('Failed to delete project')
    });
  }

  onSearch(): void   { this.page = 0; this.load(); }
  prevPage(): void   { if (this.page > 0) { this.page--; this.load(); } }
  nextPage(): void   { this.page++; this.load(); }
  hasNext(): boolean { return !!this.meta && this.page < this.meta.total_pages - 1; }

  statusLabel(s: ProjectStatus): string {
    const map: Record<ProjectStatus, string> = {
      ACTIVE: 'Active', PAUSED: 'Paused', DONE: 'Done', ARCHIVED: 'Archived'
    };
    return map[s];
  }
}
