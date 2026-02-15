import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Car } from '../../../../models/car.model';


@Component({
  selector: 'app-active-trip-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './active-trip-card.component.html',
  styleUrl: './active-trip-card.component.scss'
})
export class ActiveTripCardComponent {
  @Input() car!: Car;

  constructor(private readonly router: Router) {}

  goToTripDetail(): void {
    this.router.navigate(['/dashboard/active-trip', this.car.cnr]);
  }
}
