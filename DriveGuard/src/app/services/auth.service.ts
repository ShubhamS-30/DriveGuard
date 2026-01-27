import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface User {
  username: string;
  role: string;
}

export interface AuthResponse {
  success: boolean;
  message: string;
  user?: User;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly currentUserSubject: BehaviorSubject<User | null>;
  public currentUser$: Observable<User | null>;
  private readonly isAuthenticatedSubject: BehaviorSubject<boolean>;
  public isAuthenticated$: Observable<boolean>;

  // Hardcoded credentials (as per requirements)
  private readonly VALID_USERNAME = 'admin';
  private readonly VALID_PASSWORD = 'admin';
  private readonly VALID_ROLE = 'admin';

  constructor() {
    // Check if user exists in localStorage on initialization
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<User | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser$ = this.currentUserSubject.asObservable();

    this.isAuthenticatedSubject = new BehaviorSubject<boolean>(!!storedUser);
    this.isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  }

  /**
   * Login with username and password
   * @param username The username
   * @param password The password
   * @returns Observable with authentication response
   */
  public login(username: string, password: string): Observable<AuthResponse> {
    return new Observable(observer => {
      // Simulate API call delay
      setTimeout(() => {
        if (username === this.VALID_USERNAME && password === this.VALID_PASSWORD) {
          const user: User = {
            username: username,
            role: this.VALID_ROLE
          };

          // Store in localStorage
          localStorage.setItem('currentUser', JSON.stringify(user));
          localStorage.setItem('authToken', this.generateToken(user));

          // Update observables
          this.currentUserSubject.next(user);
          this.isAuthenticatedSubject.next(true);

          observer.next({
            success: true,
            message: 'Login successful',
            user: user
          });
          observer.complete();
        } else {
          observer.next({
            success: false,
            message: 'Invalid username or password'
          });
          observer.complete();
        }
      }, 500); // Simulate 500ms API call
    });
  }

  /**
   * Logout the current user
   */
  public logout(): void {
    localStorage.removeItem('currentUser');
    localStorage.removeItem('authToken');
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
  }

  /**
   * Get the current user
   */
  public getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Check if user is authenticated
   */
  public isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  /**
   * Get auth token
   */
  public getAuthToken(): string | null {
    return localStorage.getItem('authToken');
  }

  /**
   * Generate a mock JWT token
   */
  private generateToken(user: User): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: user.username,
      role: user.role,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60) // 24 hours
    }));
    const signature = btoa('mock-signature');
    return `${header}.${payload}.${signature}`;
  }
}
