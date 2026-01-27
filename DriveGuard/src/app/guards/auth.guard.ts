import { Injectable } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Functional guard to protect routes that require authentication
 * Usage: in routes array, add: canActivate: [authGuard]
 */
@Injectable({ providedIn: 'root' })
class AuthGuardService {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean {
    if (this.authService.isAuthenticated()) {
      return true;
    }

    // Not logged in, redirect to login page with return URL
    this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}

/**
 * Functional CanActivate guard
 */
export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  authGuardService = new AuthGuardService(
    new AuthService(),
    new Router()
  )
) => {
  return authGuardService.canActivate(route, state);
};

/**
 * Functional guard to redirect logged-in users away from auth pages
 * Usage: in login route, add: canActivate: [noAuthGuard]
 */
@Injectable({ providedIn: 'root' })
class NoAuthGuardService {
  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  canActivate(): boolean {
    if (!this.authService.isAuthenticated()) {
      return true;
    }

    // Already logged in, redirect to dashboard
    this.router.navigate(['/dashboard']);
    return false;
  }
}

export const noAuthGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  noAuthGuardService = new NoAuthGuardService(
    new AuthService(),
    new Router()
  )
) => {
  return noAuthGuardService.canActivate();
};

export { AuthGuardService, NoAuthGuardService };
