import {
  Component,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, interval, of } from 'rxjs';
import { takeUntil, switchMap, startWith, catchError } from 'rxjs/operators';
import { CarService } from '../../../services/car.service';
import { Car, CarsResponse } from '../../../models/car.model';
import { ActiveTripCardComponent } from './active-trip-card/active-trip-card.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-active-trips',
  standalone: true,
  imports: [CommonModule, ActiveTripCardComponent],
  templateUrl: './active-trips.component.html',
  styleUrl: './active-trips.component.scss',
})
export class ActiveTripsComponent implements OnInit, OnDestroy {
  @Output() backClick = new EventEmitter<void>();

  // Polling configuration
  private readonly POLLING_INTERVAL_MS = 20000; // 20 seconds

  cars: Car[] = [];
  isLoading = true;
  errorMessage: string | null = null;
  isPolling = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly carService: CarService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.startPolling();
  }

  /**
   * Start polling for active cars every 20 seconds
   * Initial fetch happens immediately, then repeats at interval
   * Polling continues even if an error occurs
   */
  private startPolling(): void {
    this.isPolling = true;

    // Use interval to emit every 20 seconds, startWith(0) to emit immediately
    interval(this.POLLING_INTERVAL_MS)
      .pipe(
        startWith(0), // Emit immediately before interval starts
        switchMap(() =>
          this.carService.getActiveCars().pipe(
            catchError((error) => {
              console.error('Error fetching active cars:', error);
              this.errorMessage = 'Failed to fetch active cars. Retrying...';
              this.isLoading = false;
              // Return empty to prevent error from propagating and terminating polling
              return of({ content: this.cars } as CarsResponse);
            }),
          ),
        ),
        takeUntil(this.destroy$),
      )
      .subscribe({
        next: (response: CarsResponse) => {
          this.cars = response.content;
          this.isLoading = false;
          // Only clear error message on successful fetch
          if (response.content.length > 0 || !this.errorMessage) {
            this.errorMessage = null;
          }
        },
      });
  }

  /**
   * Emit back click event
   */
  onBackClick(): void {
    this.router.navigate(['/dashboard']);
  }

  /**
   * Manual fetch for retry button
   */
  fetchActiveCars(): void {
    this.isLoading = true;
    this.carService.getActiveCars().subscribe({
      next: (response: CarsResponse) => {
        this.cars = response.content;
        this.isLoading = false;
        this.errorMessage = null;
      },
      error: (error) => {
        console.error('Error fetching active cars:', error);
        this.errorMessage = 'Failed to fetch active cars. Retrying...';
        this.isLoading = false;
      },
    });
  }

  /**
   * Stop polling when component is destroyed
   */
  private stopPolling(): void {
    this.isPolling = false;
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
