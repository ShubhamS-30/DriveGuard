/**
 * Car Models and Interfaces
 */

export interface Car {
  id: string;
  cnr: number;
  manufacturer: string;
  model: string;
  fuel: string;
  powerKw: number;
  transmission: string;
  weightKg: number;
  isActiveTrip: boolean;
  activeTripNumber: string;
}

export interface PageInfo {
  pageNumber: number;
  pageSize: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  offset: number;
  paged: boolean;
  unpaged: boolean;
}

export interface Sort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface CarsResponse {
  content: Car[];
  pageable: PageInfo;
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  sort: Sort;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}
