import {
  Component,
  AfterViewInit,
  OnDestroy,
  Input,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import L from 'leaflet';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss',
})
export class MapComponent implements AfterViewInit, OnDestroy, OnChanges {
  @Input() curLatitude: number = 51.505; // Default latitude
  @Input() curLongitude: number = -0.09; // Default longitude

  private map?: L.Map;
  private curMarker?: L.Marker;
  private readonly popup = L.popup();
  private readonly defaultIcon = L.icon({
    iconUrl: 'assets/leaflet/marker-icon.png',
    iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
    shadowUrl: 'assets/leaflet/marker-shadow.png', // This empty string disables the shadow request
  });

  ngAfterViewInit(): void {
    this.initMap();
  }

  private initMap(): void {
    this.map = L.map('map', {
      center: [this.curLatitude, this.curLongitude],
      zoom: 13,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    // Initialize the single marker instance
    this.curMarker = L.marker([this.curLatitude, this.curLongitude], {
      icon: this.defaultIcon,
    }).addTo(this.map);

    this.updateMarkerPopup();

    this.map.on('click', (e: L.LeafletMouseEvent) => this.onMapClick(e));
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Only update if the map is initialized and inputs actually changed
    if (
      this.map &&
      this.curMarker &&
      (changes['curLatitude'] || changes['curLongitude'])
    ) {
      const newLat = this.curLatitude;
      const newLng = this.curLongitude;

      // 1. Move the existing marker to the new coordinates
      this.curMarker.setLatLng([newLat, newLng]);

      // 2. Center the map on the new location
      this.map.setView([newLat, newLng], this.map.getZoom());

      // 3. Update the text inside the popup
      this.updateMarkerPopup();
    }
  }

  private updateMarkerPopup(): void {
    if (this.curMarker) {
      this.curMarker.bindPopup(
        `<b>Current location:</b><br>${this.curLatitude.toFixed(4)}, ${this.curLongitude.toFixed(4)}`,
      );
    }
  }

  // Good practice: Clean up the map instance when the component is destroyed
  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  onMapClick(e: L.LeafletMouseEvent): void {
    const position = e.latlng;
    this.popup
      .setLatLng(e.latlng)
      .setContent(
        `You clicked location: ${position.lat.toFixed(4)}, ${position.lng.toFixed(4)}`,
      )
      .openOn(this.map!);
  }
}
