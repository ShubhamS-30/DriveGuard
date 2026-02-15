import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { LocationData } from '../models/location.model';

@Injectable({
  providedIn: 'root',
})
export class WebSocketService {
  private ws: WebSocket | null = null;
  private readonly locationDataSubject =
    new BehaviorSubject<LocationData | null>(null);
  private readonly connectionStatusSubject = new BehaviorSubject<boolean>(
    false,
  );
  private readonly errorSubject = new Subject<string>();

  public locationData$: Observable<LocationData | null> =
    this.locationDataSubject.asObservable();
  public connectionStatus$: Observable<boolean> =
    this.connectionStatusSubject.asObservable();
  public error$: Observable<string> = this.errorSubject.asObservable();

  constructor() {}

  /**
   * Connect to WebSocket and receive location data
   */
  connect(carId: string): Observable<LocationData | null> {
    const wsUrl = `ws://localhost:8082/ws/locations/${carId}`;

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        console.log('WebSocket connected');
        this.connectionStatusSubject.next(true);
      };

      this.ws.onmessage = (event: MessageEvent) => {
        try {
          const data = event.data;
          // 1. Ignore the 'Connected' handshake string
          if (typeof data === 'string' && data.startsWith('Connected')) {
            return; // Exit early without parsing
          }
          const locationData: LocationData = JSON.parse(data);
          this.locationDataSubject.next(locationData);
        } catch (error) {
          console.error('Error parsing WebSocket message:', error);
          this.errorSubject.next('Failed to parse location data');
        }
      };

      this.ws.onerror = (error: Event) => {
        console.error('WebSocket error:', error);
        this.connectionStatusSubject.next(false);
        this.errorSubject.next('WebSocket connection error');
      };

      this.ws.onclose = () => {
        console.log('WebSocket disconnected');
        this.connectionStatusSubject.next(false);
      };
    } catch (error) {
      console.error('Error creating WebSocket:', error);
      this.errorSubject.next('Failed to create WebSocket connection');
      this.connectionStatusSubject.next(false);
    }

    return this.locationData$;
  }

  /**
   * Disconnect WebSocket
   */
  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
      this.connectionStatusSubject.next(false);
    }
  }

  /**
   * Get current location data
   */
  getCurrentLocationData(): LocationData | null {
    return this.locationDataSubject.value;
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connectionStatusSubject.value;
  }
}
