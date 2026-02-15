import { Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { ActiveTripDetailComponent } from './pages/dashboard/active-trip-detail/active-trip-detail.component';
import { authGuard, noAuthGuard } from './guards/auth.guard';
import { ActiveTripsComponent } from './pages/dashboard/active-trips/active-trips.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [noAuthGuard]
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'active-trips',
        component: ActiveTripsComponent,
        canActivate: [authGuard]
      },
      {
        path: 'active-trip/:carId',
        component: ActiveTripDetailComponent,
        canActivate: [authGuard]
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
