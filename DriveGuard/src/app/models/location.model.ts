/**
 * Location and Trip Data Models
 */

export interface LocationData {
  latitude: string;
  longitude: string;
  target_speed: string;
  way_Maxspeed: string | null;
  speed_osrm: string;
  elevation: string;
  fwd_azimuth: string;
  way_type: string;
  way_surface: string;
  node_intersection: string | null;
  node_railway: string | null;
  node_crossing: string | null;
  node_highway: string | null;
  node_stop: string | null;
  start_stop: string;
  azimuth_diff: string;
  elevation_diff: string;
  carId: string;
  tripNumber: string;
  timestamp: string;
  tripCompletion: number;
}
