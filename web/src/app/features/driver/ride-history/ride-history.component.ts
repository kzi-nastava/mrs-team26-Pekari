import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ride-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-history.component.html',
  styleUrls: ['./ride-history.component.css']
})
export class RideHistoryComponent {
  rides = [
    {
      date: '15 December 2024',
      time: '14:30 - 15:15 (45 min)',
      status: ['completed', 'panic'],
      locations: [
        { type: 'start', label: 'Start', address: 'Knez Mihailova 12, Beograd' },
        { type: 'end', label: 'Finish', address: 'Aerodrom Nikola Tesla, Beograd' }
      ],
      distance: '18.5 km',
      price: '1,250 RSD',
      passengers: [
        { initials: 'MJ', name: 'Marko Jovanović' },
        { initials: 'AP', name: 'Ana Petrović' }
      ]
    },
    {
      date: '14 December 2024',
      time: '09:15 - 09:45 (30 min)',
      status: ['completed'],
      locations: [
        { type: 'start', label: 'Start', address: 'Slavija trg 1, Beograd' },
        { type: 'end', label: 'Finish', address: 'Ušće Shopping Center, Beograd' }
      ],
      distance: '8.2 km',
      price: '650 RSD',
      passengers: [
        { initials: 'SM', name: 'Stefan Marković' }
      ]
    },
    {
      date: '13 December 2024.',
      time: 'Canceled',
      status: ['cancelled-driver'],
      locations: [
        { type: 'start', label: 'Start', address: 'Terazije 25, Beograd' },
        { type: 'end', label: 'Finish', address: 'Ada Ciganlija, Beograd' }
      ],
      cancelReason: 'Technical issues',
      passengers: [
        { initials: 'NM', name: 'Nikola Milić' },
        { initials: 'JD', name: 'Jelena Dimitrijević' },
        { initials: 'DV', name: 'Dušan Vasić' }
      ]
    },
    {
      date: '12 December 2024',
      time: 'Canceled',
      status: ['cancelled-passenger'],
      locations: [
        { type: 'start', label: 'Start', address: 'Studentski trg 1, Beograd' },
        { type: 'end', label: 'Finish', address: 'Kalemegdan, Beograd' }
      ],
      cancelReason: 'Change of plans',
      passengers: [
        { initials: 'MP', name: 'Milan Popović' }
      ]
    }
  ];
}
