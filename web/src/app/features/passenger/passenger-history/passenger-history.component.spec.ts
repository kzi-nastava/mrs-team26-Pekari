import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { PassengerHistoryComponent } from './passenger-history.component';
import { RideApiService, PassengerRideHistoryResponse, PaginatedResponse } from '../../../core/services/ride-api.service';
import { ChangeDetectorRef } from '@angular/core';
import { of, throwError, delay } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('PassengerHistoryComponent', () => {
  let component: PassengerHistoryComponent;
  let fixture: ComponentFixture<PassengerHistoryComponent>;
  let rideApiServiceSpy: jasmine.SpyObj<RideApiService>;

  const mockRidesResponse: PaginatedResponse<PassengerRideHistoryResponse> = {
    content: [
      {
        id: 1,
        startTime: new Date().toISOString(),
        endTime: new Date().toISOString(),
        status: 'COMPLETED',
        price: 500,
        vehicleType: 'SEDAN',
        babyTransport: false,
        petTransport: false,
        pickupLocation: 'Address 1',
        dropoffLocation: 'Address 2',
        distanceKm: 5,
        panicActivated: false,
        stops: [],
        cancelled: false,
        cancelledBy: null,
        driver: null,
      }
    ],
    page: 0,
    size: 10,
    totalElements: 1,
  };

  beforeEach(async () => {
    rideApiServiceSpy = jasmine.createSpyObj('RideApiService', [
      'getPassengerRideHistory',
      'getFavoriteRoutes',
      'rateRide'
    ]);

    rideApiServiceSpy.getPassengerRideHistory.and.returnValue(of(mockRidesResponse));
    rideApiServiceSpy.getFavoriteRoutes.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        FormsModule,
        PassengerHistoryComponent
      ],
      providers: [
        { provide: RideApiService, useValue: rideApiServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PassengerHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Rating Modal', () => {
    let mockRide: any;

    beforeEach(() => {
      mockRide = {
        id: 1,
        isRatable: true,
        locations: [{ address: 'A' }, { address: 'B' }]
      };
    });

    it('should open rating modal when openRatingModal is called', () => {
      component.openRatingModal(mockRide);
      expect(component.showRatingModal).toBeTrue();
      expect(component.completedRideId).toBe(1);
      expect(component.vehicleRating).toBe(0);
      expect(component.driverRating).toBe(0);
      expect(component.ratingComment).toBe('');
    });

    it('should set vehicle rating', () => {
      component.setVehicleRating(4);
      expect(component.vehicleRating).toBe(4);
    });

    it('should set driver rating', () => {
      component.setDriverRating(5);
      expect(component.driverRating).toBe(5);
    });

    it('should update isRatable correctly based on time', () => {
      // 1 day ago - should be ratable
      const oneDayAgo = new Date();
      oneDayAgo.setDate(oneDayAgo.getDate() - 1);
      const ride1 = { status: 'COMPLETED', endTime: oneDayAgo.toISOString(), price: 500 } as any;
      const mapped1 = (component as any).mapRideForDisplay(ride1);
      expect(mapped1.isRatable).toBeTrue();

      // 4 days ago - should not be ratable
      const fourDaysAgo = new Date();
      fourDaysAgo.setDate(fourDaysAgo.getDate() - 4);
      const ride2 = { status: 'COMPLETED', endTime: fourDaysAgo.toISOString(), price: 500 } as any;
      const mapped2 = (component as any).mapRideForDisplay(ride2);
      expect(mapped2.isRatable).toBeFalse();

      // Cancelled ride - should not be ratable
      const ride3 = { status: 'CANCELLED', endTime: oneDayAgo.toISOString(), price: 500 } as any;
      const mapped3 = (component as any).mapRideForDisplay(ride3);
      expect(mapped3.isRatable).toBeFalse();
    });

    it('should close rating modal when closeRatingModal is called', () => {
      component.showRatingModal = true;
      component.closeRatingModal();
      expect(component.showRatingModal).toBeFalse();
      expect(component.completedRideId).toBeUndefined();
    });

    it('should call closeRatingModal when skipRating is called', () => {
      spyOn(component, 'closeRatingModal');
      component.skipRating();
      expect(component.closeRatingModal).toHaveBeenCalled();
    });

    describe('submitRating', () => {
      it('should show error if ratings are not provided', () => {
        component.completedRideId = 1;
        component.vehicleRating = 0;
        component.driverRating = 0;

        component.submitRating();

        expect(component.error).toBe('Please provide both vehicle and driver ratings');
        expect(rideApiServiceSpy.rateRide).not.toHaveBeenCalled();
      });

    it('should call rateRide with correct data and handle success', fakeAsync(() => {
        component.completedRideId = 1;
        component.vehicleRating = 4;
        component.driverRating = 5;
        component.ratingComment = 'Great ride!';

        // Use delay to ensure ratingSubmitting is true during the request
        rideApiServiceSpy.rateRide.and.returnValue(of({ message: 'Success' }).pipe(delay(100)));
        spyOn(component, 'closeRatingModal');

        component.submitRating();

        expect(component.ratingSubmitting).toBeTrue();
        expect(rideApiServiceSpy.rateRide).toHaveBeenCalledWith(1, {
          vehicleRating: 4,
          driverRating: 5,
          comment: 'Great ride!'
        });

        tick(100); // Handle observable delay

        expect(component.ratingSubmitting).toBeFalse();
        expect(component.ratingSuccess).toBe('Success');

        tick(2000); // Handle setTimeout
        expect(component.closeRatingModal).toHaveBeenCalled();
      }));

      it('should handle error when rateRide fails', () => {
        component.completedRideId = 1;
        component.vehicleRating = 4;
        component.driverRating = 5;

        const errorResponse = { error: { message: 'Already rated' } };
        rideApiServiceSpy.rateRide.and.returnValue(throwError(() => errorResponse));

        component.submitRating();

        expect(component.ratingSubmitting).toBeFalse();
        expect(component.error).toBe('Already rated');
        expect(component.ratingSuccess).toBeUndefined();
      });

      it('should use default error message if error response has no message', () => {
        component.completedRideId = 1;
        component.vehicleRating = 4;
        component.driverRating = 5;

        rideApiServiceSpy.rateRide.and.returnValue(throwError(() => new Error('Network error')));

        component.submitRating();

        expect(component.error).toBe('Failed to submit rating');
      });
    });
  });

  describe('UI Interaction', () => {
    it('should call openRatingModal when rate button is clicked', () => {
      spyOn(component, 'openRatingModal');

      // Setup a ratable ride in the list
      component.ridesList = [{
        id: 10,
        isRatable: true,
        status: ['completed'],
        date: '1 Jan 2024',
        time: '10:00 - 10:30',
        locations: [
          { type: 'start', address: 'Start' },
          { type: 'end', address: 'End' }
        ]
      }];

      fixture.detectChanges();

      const rateBtn = fixture.nativeElement.querySelector('.rate-btn');
      expect(rateBtn).toBeTruthy();

      rateBtn.click();

      expect(component.openRatingModal).toHaveBeenCalledWith(component.ridesList[0]);
    });

    it('should call skipRating when clicking on modal overlay', () => {
      spyOn(component, 'skipRating');
      component.showRatingModal = true;
      fixture.detectChanges();

      const overlay = fixture.nativeElement.querySelector('.modal-overlay');
      overlay.click();

      expect(component.skipRating).toHaveBeenCalled();
    });

    it('should disable submit button when ratings are not set', () => {
      component.showRatingModal = true;
      component.vehicleRating = 0;
      component.driverRating = 0;
      fixture.detectChanges();

      const submitBtn = fixture.nativeElement.querySelector('.primary-btn');
      expect(submitBtn.disabled).toBeTrue();
    });

    it('should enable submit button when ratings are set', () => {
      component.showRatingModal = true;
      component.completedRideId = 1;
      component.vehicleRating = 4;
      component.driverRating = 5;
      fixture.detectChanges();

      const submitBtn = fixture.nativeElement.querySelector('.primary-btn');
      expect(submitBtn.disabled).toBeFalse();
    });

    it('should update vehicle rating when star is clicked', () => {
      component.showRatingModal = true;
      fixture.detectChanges();

      const vehicleStars = fixture.nativeElement.querySelectorAll('.rating-group:first-of-type .star-btn');
      vehicleStars[3].click(); // Click 4th star (index 3)

      expect(component.vehicleRating).toBe(4);
    });

    it('should update driver rating when star is clicked', () => {
      component.showRatingModal = true;
      fixture.detectChanges();

      const driverStars = fixture.nativeElement.querySelectorAll('.rating-group:nth-of-type(2) .star-btn');
      driverStars[4].click(); // Click 5th star (index 4)

      expect(component.driverRating).toBe(5);
    });
  });

  describe('General Component Logic', () => {
    it('should handle error when loadRides fails', () => {
      rideApiServiceSpy.getPassengerRideHistory.and.returnValue(throwError(() => new Error('API Error')));
      component.loadRides();
      expect(component.error).toBe('Failed to load ride history');
      expect(component.loading).toBeFalse();
    });

    it('should sort rides by price', () => {
      component.ridesList = [
        { rawRide: { price: 100 }, price: '100 RSD', status: ['completed'], locations: [{ address: 'A' }, { address: 'B' }] },
        { rawRide: { price: 300 }, price: '300 RSD', status: ['completed'], locations: [{ address: 'C' }, { address: 'D' }] },
        { rawRide: { price: 200 }, price: '200 RSD', status: ['completed'], locations: [{ address: 'E' }, { address: 'F' }] }
      ];
      component.sortField = 'price';
      component.sortDirection = 'asc';

      (component as any).sortRides();

      expect(component.ridesList[0].rawRide.price).toBe(100);
      expect(component.ridesList[1].rawRide.price).toBe(200);
      expect(component.ridesList[2].rawRide.price).toBe(300);

      component.toggleSortDirection(); // Switch to desc
      expect(component.ridesList[0].rawRide.price).toBe(300);
    });
  });
});
