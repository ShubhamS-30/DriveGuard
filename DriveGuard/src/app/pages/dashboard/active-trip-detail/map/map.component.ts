import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import L from 'leaflet';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss',
})
export class MapComponent implements AfterViewInit, OnDestroy {
  private map?: L.Map;
  private readonly popup = L.popup();
  private readonly defaultIcon = L.icon({
    iconUrl: 'assets/leaflet/marker-icon.png',
    iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
    shadowUrl: 'assets/leaflet/marker-shadow.png', // This empty string disables the shadow request
  });

  // Wait until the DOM is fully rendered
  ngAfterViewInit(): void {
    this.initMap();
  }

  private initMap(): void {
    // 1. Initialize the map object
    this.map = L.map('map', {
      center: [51.505, -0.09],
      zoom: 13,
    });

    // 2. Add the OpenStreetMap tiles
    const tiles = L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors',
      },
    );

    const marker = L.marker([51.5, -0.09], { icon: this.defaultIcon }).addTo(
      this.map,
    );
    const position = marker.getLatLng();
    marker.bindPopup(
      `Current location: ${position.lat.toFixed(4)}, ${position.lng.toFixed(4)}`,
    );

    tiles.addTo(this.map);
    this.map.on('click', (e: L.LeafletMouseEvent) => this.onMapClick(e));
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
      .setContent( `You clicked location: ${position.lat.toFixed(4)}, ${position.lng.toFixed(4)}`)
      .openOn(this.map!);
  }
}
