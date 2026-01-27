import { Component, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { User } from '../../../services/auth.service';

@Component({
  selector: 'app-dashboard-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-header.component.html',
  styleUrl: './dashboard-header.component.scss'
})
export class DashboardHeaderComponent {
  @Input() currentUser: User | null = null;
  @Output() logoutClick = new EventEmitter<void>();

  onLogout(): void {
    this.logoutClick.emit();
  }
}
