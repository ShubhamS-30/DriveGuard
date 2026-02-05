import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService, User } from '../../services/auth.service';
import { DashboardHeaderComponent } from './dashboard-header/dashboard-header.component';
import { FeatureCardComponent, Feature } from './feature-card/feature-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DashboardHeaderComponent, FeatureCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  loginTime: Date = new Date();
  features: Feature[] = [
    {
      icon: 'geo-alt',
      type: 'location',
      title: 'Real-time Locations',
      description: 'Monitor vehicle locations via WebSocket streaming'
    },
    {
      icon: 'exclamation-triangle',
      type: 'alert',
      title: 'Safety Alerts',
      description: 'Receive real-time alerts for detected anomalies'
    },
    {
      icon: 'list-check',
      type: 'rules',
      title: 'Safety Rules',
      description: 'Speeding detection, dangerous turning & more'
    },
    {
      icon: 'graph-up',
      type: 'data',
      title: 'Telemetry Data',
      description: 'Access vehicle telemetry and trip information'
    }
  ];
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to current user
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
      });

    // If user is not authenticated, redirect to login
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
    }
  }

  /**
   * Handle logout
   */
  onLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
