import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { LocationData } from '../../../models/location.model';
import { WebSocketService } from '../../../services/websocket.service';
import { MapComponent } from './map/map.component';

@Component({
  selector: 'app-active-trip-detail',
  standalone: true,
  imports: [CommonModule,MapComponent],
  templateUrl: './active-trip-detail.component.html',
  styleUrl: './active-trip-detail.component.scss'
})
export class ActiveTripDetailComponent implements OnInit, OnDestroy {
  carId: string = '';
  locationData: LocationData | null = null;
  isConnected: boolean = false;
  error: string | null = null;
  lastUpdateTime: Date | null = null;

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    // Get carId from route parameters
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      this.carId = params['carId'];
      if (this.carId) {
        this.connectToWebSocket();
      } else {
        this.router.navigate(['/dashboard']);
      }
    });

    // Subscribe to location data updates
    this.webSocketService.locationData$
      .pipe(takeUntil(this.destroy$))
      .subscribe((data : any) => {
        this.locationData = data;
        if (data) {
          this.lastUpdateTime = new Date();
        }
      });

    // Subscribe to connection status
    this.webSocketService.connectionStatus$
      .pipe(takeUntil(this.destroy$))
      .subscribe((status : any) => {
        this.isConnected = status;
      });

    // Subscribe to errors
    this.webSocketService.error$
      .pipe(takeUntil(this.destroy$))
      .subscribe((errorMsg : any) => {
        this.error = errorMsg;
      });
  }

  private connectToWebSocket(): void {
    this.webSocketService.connect(this.carId).pipe(takeUntil(this.destroy$)).subscribe();
  }

  goBack(): void {
    this.router.navigate(['/dashboard/active-trips']);
  }

  ngOnDestroy(): void {
    this.webSocketService.disconnect();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
