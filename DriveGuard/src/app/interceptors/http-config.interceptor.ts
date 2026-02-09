import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class HttpConfigInterceptor implements HttpInterceptor {
  constructor() {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Clone the request and add headers
    const clonedRequest = request.clone({
      setHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      withCredentials: false // Set to true if your backend requires cookies
    });

    return next.handle(clonedRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle CORS errors
        if (error.status === 0) {
          console.error('CORS Error or Network Error:', error);
          return throwError(
            () => new Error('CORS Error: Unable to reach the server. Please ensure the backend has CORS enabled.')
          );
        }
        return throwError(() => error);
      })
    );
  }
}
