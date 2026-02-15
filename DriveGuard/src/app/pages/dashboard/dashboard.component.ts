import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterOutlet, UrlSegment } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AuthService, User } from '../../services/auth.service';
import { DashboardHeaderComponent } from './dashboard-header/dashboard-header.component';
import { FeatureCardComponent, Feature } from './feature-card/feature-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DashboardHeaderComponent, FeatureCardComponent, RouterOutlet],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  loginTime: Date = new Date();
  showDashboard = true;
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
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Subscribe to current user
    this.authService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.currentUser = user;
      });

    // Detect route changes to show/hide active trips
    this.activatedRoute.firstChild?.url
      .pipe(takeUntil(this.destroy$))
      .subscribe(url => {
        console.log('Route changed, current URL:', url);
        this.showDashboard = this.showDashboardCards(url);
      });

    // If user is not authenticated, redirect to login
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
    }
  }

  /**
   * Handle feature card click
   */
  onFeatureCardClick(feature: Feature): void {
    if (feature.type === 'location') {
      this.showDashboard = false;
      this.router.navigate(['/dashboard/active-trips']);
    }
  }

  /**
   * Handle back from active trips
   */
  onBackToMain(): void {
    this.showDashboard = true;
    this.router.navigate(['/dashboard']);
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

  showDashboardCards(urlSegment: UrlSegment[]): boolean {
    if(!urlSegment || urlSegment.length === 0) {
      return true;
    }
    switch (urlSegment[0].path) {
      case 'active-trips':
      case 'active-trip':
        return false;
      default:
        return true;
    }
  }
}
