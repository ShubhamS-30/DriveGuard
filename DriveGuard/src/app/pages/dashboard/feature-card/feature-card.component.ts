import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Feature {
  icon: string;
  type: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-feature-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './feature-card.component.html',
  styleUrl: './feature-card.component.scss'
})
export class FeatureCardComponent {
  @Input() icon: string = '';
  @Input() type: string = '';
  @Input() title: string = '';
  @Input() description: string = '';
  @Output() cardClicked = new EventEmitter<Feature>();

  onCardClick(): void {
    this.cardClicked.emit({
      icon: this.icon,
      type: this.type,
      title: this.title,
      description: this.description
    });
  }
}
