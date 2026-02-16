/**
 * Location and Trip Data Models
 */

export interface LocationData {
  latitude: number;
  longitude: number;
  target_speed: number;
  way_Maxspeed: number | null;
  speed_osrm: number;
  elevation: number;
  fwd_azimuth:  number;
  way_type: string;
  way_surface: string;
  node_intersection: string | null;
  node_railway: string | null;
  node_crossing: string | null;
  node_highway: string | null;
  node_stop: string | null;
  start_stop: string;
  azimuth_diff:  number;
  elevation_diff:  number;
  carId:  number;
  tripNumber: string;
  timestamp: string;
  tripCompletion:  number;
}
