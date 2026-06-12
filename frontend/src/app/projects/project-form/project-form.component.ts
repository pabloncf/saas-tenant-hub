import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProjectService } from '../../core/services/project.service';

@Component({
  selector: 'app-project-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './project-form.component.html',
  styleUrl: './project-form.component.scss'
})
export class ProjectFormComponent implements OnInit {
  private fb      = inject(FormBuilder);
  private route   = inject(ActivatedRoute);
  private router  = inject(Router);
  private service = inject(ProjectService);

  isEdit  = false;
  id      = '';
  loading = false;
  error   = '';

  form = this.fb.group({
    name:        ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    status:      ['ACTIVE']
  });

  ngOnInit(): void {
    this.id     = this.route.snapshot.paramMap.get('id') ?? '';
    this.isEdit = !!this.id;

    if (this.isEdit) {
      this.service.get(this.id).subscribe({
        next: res => this.form.patchValue(res.data as any),
        error: () => this.error = 'Project not found'
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error   = '';

    const payload = this.form.getRawValue() as any;
    const op = this.isEdit
      ? this.service.update(this.id, payload)
      : this.service.create(payload);

    op.subscribe({
      next: () => this.router.navigate(['/projects']),
      error: err => {
        this.error   = err.error?.message ?? err.error?.errors ?? 'Save failed';
        this.loading = false;
      }
    });
  }
}
