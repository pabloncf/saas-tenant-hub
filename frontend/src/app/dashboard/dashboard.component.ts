import { Component, OnInit, inject } from '@angular/core';
import { ChartData, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { ProjectService } from '../core/services/project.service';
import { Project, ProjectStatus } from '../core/models/project.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [BaseChartDirective],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private projectService = inject(ProjectService);

  projects: Project[] = [];
  loading = true;

  stats = { total: 0, active: 0, paused: 0, done: 0 };

  chartData: ChartData<'doughnut'> = {
    labels: ['Active', 'Paused', 'Done', 'Archived'],
    datasets: [{
      data: [0, 0, 0, 0],
      backgroundColor: ['#30d158', '#ff9f0a', '#5b4cf5', '#8e8e93'],
      borderWidth: 0,
      hoverOffset: 4
    }]
  };

  chartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    cutout: '72%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: { padding: 16, font: { size: 12 } }
      }
    }
  };

  ngOnInit(): void {
    this.projectService.list(0, 100).subscribe({
      next: res => {
        this.projects = res.data;
        this.computeStats();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  private computeStats(): void {
    const count = (s: ProjectStatus) => this.projects.filter(p => p.status === s).length;
    this.stats = {
      total:  this.projects.length,
      active: count('ACTIVE'),
      paused: count('PAUSED'),
      done:   count('DONE')
    };
    this.chartData = {
      ...this.chartData,
      datasets: [{
        ...this.chartData.datasets[0],
        data: [this.stats.active, this.stats.paused, this.stats.done,
               this.projects.filter(p => p.status === 'ARCHIVED').length]
      }]
    };
  }
}
