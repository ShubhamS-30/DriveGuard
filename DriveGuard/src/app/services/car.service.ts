import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS, DEFAULT_PAGINATION } from '../config/api.endpoints';
import { Car, CarsResponse } from '../models/car.model';

@Injectable({
  providedIn: 'root'
})
export class CarService {
  constructor(private readonly http: HttpClient) {}

  /**
   * Get all active cars with pagination and sorting
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param sort - Sort parameter (e.g., 'cnr,asc')
   * @returns Observable of CarsResponse
   */
  getActiveCars(
    page: number = DEFAULT_PAGINATION.page,
    size: number = DEFAULT_PAGINATION.size,
    sort: string = DEFAULT_PAGINATION.sort
  ): Observable<CarsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<CarsResponse>(
      `${API_ENDPOINTS.BASE_URL}${API_ENDPOINTS.CARS.ACTIVE}`,
      { params }
    );
  }

  /**
   * Get all cars paginated
   * @param page - Page number (0-indexed)
   * @param size - Page size
   * @param sort - Sort parameter
   * @returns Observable of CarsResponse
   */
  getCars(
    page: number = DEFAULT_PAGINATION.page,
    size: number = DEFAULT_PAGINATION.size,
    sort: string = DEFAULT_PAGINATION.sort
  ): Observable<CarsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.get<CarsResponse>(
      `${API_ENDPOINTS.BASE_URL}${API_ENDPOINTS.CARS.GET_ALL}`,
      { params }
    );
  }

  /**
   * Get car by CNR
   * @param cnr - Car number
   * @returns Observable of Car
   */
  getCarByCnr(cnr: number): Observable<Car> {
    return this.http.get<Car>(
      `${API_ENDPOINTS.BASE_URL}${API_ENDPOINTS.CARS.GET_BY_CNR(cnr)}`
    );
  }

  /**
   * Create a new car
   * @param car - Car object to create
   * @returns Observable of created Car
   */
  createCar(car: Partial<Car>): Observable<Car> {
    return this.http.post<Car>(
      `${API_ENDPOINTS.BASE_URL}${API_ENDPOINTS.CARS.CREATE}`,
      car
    );
  }

  /**
   * Update a car
   * @param cnr - Car number
   * @param car - Updated car object
   * @returns Observable of updated Car
   */
  updateCar(cnr: number, car: Partial<Car>): Observable<Car> {
    return this.http.put<Car>(
      `${API_ENDPOINTS.BASE_URL}${API_ENDPOINTS.CARS.UPDATE(cnr)}`,
      car
    );
  }

  /**
   * Delete a car
   * @param cnr - Car number
   * @returns Observable of void
   */
  deleteCar(cnr: number): Observable<void> {
    return this.http.delete<void>(
      `${API_ENDPOINTS.BASE_URL}${API_ENDPOINTS.CARS.DELETE(cnr)}`
    );
  }
}
