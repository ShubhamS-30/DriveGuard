/**
 * API Endpoints Configuration
 * Centralized location for all API endpoint URLs
 */

export const API_ENDPOINTS = {
  // Base URL
  BASE_URL: 'http://localhost:8080',

  // Car endpoints
  CARS: {
    ACTIVE: '/cars/active',
    GET_ALL: '/cars',
    GET_BY_CNR: (cnr: number) => `/cars/${cnr}`,
    CREATE: '/cars',
    UPDATE: (cnr: number) => `/cars/${cnr}`,
    DELETE: (cnr: number) => `/cars/${cnr}`
  },

  // Trip endpoints
  TRIPS: {
    ACTIVE: '/trips/active',
    GET_ALL: '/trips',
    GET_BY_ID: (id: string) => `/trips/${id}`
  }
};

/**
 * Default query parameters for pagination and sorting
 */
export const DEFAULT_PAGINATION = {
  page: 0,
  size: 20,
  sort: 'cnr,asc'
};
