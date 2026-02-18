import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AddDriverComponent } from './add-driver.component';
import { AuthService } from '../../../core/services/auth.service';
import { of, throwError, Subject } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

const validDriverFormValues = {
  firstName: 'John',
  lastName: 'Doe',
  email: 'john@example.com',
  address: '123 Main St',
  phoneNumber: '+381601234567',
  vehicleModel: 'Tesla Model 3',
  vehicleType: 'STANDARD',
  licensePlate: 'BG-123-AA',
  numberOfSeats: 4,
  babyFriendly: false,
  petFriendly: false
};

describe('AddDriverComponent', () => {
  let component: AddDriverComponent;
  let fixture: ComponentFixture<AddDriverComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['registerDriver']);

    await TestBed.configureTestingModule({
      imports: [AddDriverComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    fixture = TestBed.createComponent(AddDriverComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize form with all required fields', () => {
      expect(component.driverForm.contains('firstName')).toBeTruthy();
      expect(component.driverForm.contains('lastName')).toBeTruthy();
      expect(component.driverForm.contains('email')).toBeTruthy();
      expect(component.driverForm.contains('address')).toBeTruthy();
      expect(component.driverForm.contains('phoneNumber')).toBeTruthy();
      expect(component.driverForm.contains('vehicleModel')).toBeTruthy();
      expect(component.driverForm.contains('vehicleType')).toBeTruthy();
      expect(component.driverForm.contains('licensePlate')).toBeTruthy();
      expect(component.driverForm.contains('numberOfSeats')).toBeTruthy();
      expect(component.driverForm.contains('babyFriendly')).toBeTruthy();
      expect(component.driverForm.contains('petFriendly')).toBeTruthy();
    });

    it('should initialize form with correct initial values', () => {
      expect(component.driverForm.get('firstName')?.value).toBe('');
      expect(component.driverForm.get('lastName')?.value).toBe('');
      expect(component.driverForm.get('email')?.value).toBe('');
      expect(component.driverForm.get('address')?.value).toBe('');
      expect(component.driverForm.get('phoneNumber')?.value).toBe('');
      expect(component.driverForm.get('vehicleModel')?.value).toBe('');
      expect(component.driverForm.get('vehicleType')?.value).toBe('STANDARD');
      expect(component.driverForm.get('licensePlate')?.value).toBe('');
      expect(component.driverForm.get('numberOfSeats')?.value).toBe(4);
      expect(component.driverForm.get('babyFriendly')?.value).toBe(false);
      expect(component.driverForm.get('petFriendly')?.value).toBe(false);
    });

    it('should have invalid form initially', () => {
      expect(component.driverForm.valid).toBeFalsy();
    });

    it('should initialize error and success message signals as null and isSubmitting as false', () => {
      expect(component.errorMessage()).toBeNull();
      expect(component.successMessage()).toBeNull();
      expect(component.isSubmitting()).toBe(false);
    });
  });

  describe('Required Field Validations', () => {
    it('should validate firstName as required', () => {
      const control = component.driverForm.get('firstName');
      expect(control?.hasError('required')).toBeTruthy();
      control?.setValue('John');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate lastName as required', () => {
      const control = component.driverForm.get('lastName');
      expect(control?.hasError('required')).toBeTruthy();
      control?.setValue('Doe');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate email as required', () => {
      const control = component.driverForm.get('email');
      expect(control?.hasError('required')).toBeTruthy();
      control?.setValue('test@example.com');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate address as required', () => {
      const control = component.driverForm.get('address');
      expect(control?.hasError('required')).toBeTruthy();
      control?.setValue('123 Main St');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate phoneNumber as required', () => {
      const control = component.driverForm.get('phoneNumber');
      expect(control?.hasError('required')).toBeTruthy();
      control?.setValue('+381601234567');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate vehicleModel as required', () => {
      const control = component.driverForm.get('vehicleModel');
      expect(control?.hasError('required')).toBeTruthy();
      control?.setValue('Tesla Model 3');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate licensePlate as required', () => {
      const control = component.driverForm.get('licensePlate');
      expect(control?.hasError('required')).toBeTruthy();
      control?.setValue('BG-123-AA');
      expect(control?.hasError('required')).toBeFalsy();
    });
  });

  describe('MinLength Validations', () => {
    it('should reject firstName shorter than 2 characters', () => {
      const control = component.driverForm.get('firstName');
      control?.setValue('A');
      expect(control?.hasError('minlength')).toBeTruthy();
    });

    it('should accept firstName with 2 or more characters', () => {
      const control = component.driverForm.get('firstName');
      control?.setValue('Ab');
      expect(control?.hasError('minlength')).toBeFalsy();
    });

    it('should reject lastName shorter than 2 characters', () => {
      const control = component.driverForm.get('lastName');
      control?.setValue('X');
      expect(control?.hasError('minlength')).toBeTruthy();
    });

    it('should accept lastName with 2 or more characters', () => {
      const control = component.driverForm.get('lastName');
      control?.setValue('Ab');
      expect(control?.hasError('minlength')).toBeFalsy();
    });
  });

  describe('Email Validation', () => {
    it('should accept valid email format', () => {
      const control = component.driverForm.get('email');
      control?.setValue('user@example.com');
      expect(control?.hasError('email')).toBeFalsy();
    });

    it('should reject email without @ symbol', () => {
      const control = component.driverForm.get('email');
      control?.setValue('userexample.com');
      expect(control?.hasError('email')).toBeTruthy();
    });

    it('should reject email without domain', () => {
      const control = component.driverForm.get('email');
      control?.setValue('user@');
      expect(control?.hasError('email')).toBeTruthy();
    });

    it('should reject email without username', () => {
      const control = component.driverForm.get('email');
      control?.setValue('@example.com');
      expect(control?.hasError('email')).toBeTruthy();
    });
  });

  describe('Phone Number Validation', () => {
    it('should accept valid phone number with 10 digits', () => {
      const control = component.driverForm.get('phoneNumber');
      control?.setValue('6012345678');
      expect(control?.hasError('pattern')).toBeFalsy();
    });

    it('should accept valid phone number with plus and digits', () => {
      const control = component.driverForm.get('phoneNumber');
      control?.setValue('+381601234567');
      expect(control?.hasError('pattern')).toBeFalsy();
    });

    it('should reject phone number with letters', () => {
      const control = component.driverForm.get('phoneNumber');
      control?.setValue('+3816012345a');
      expect(control?.hasError('pattern')).toBeTruthy();
    });

    it('should reject phone number shorter than 10 digits', () => {
      const control = component.driverForm.get('phoneNumber');
      control?.setValue('123456789');
      expect(control?.hasError('pattern')).toBeTruthy();
    });
  });

  describe('Number of Seats Validation', () => {
    it('should accept numberOfSeats 1 (min boundary)', () => {
      const control = component.driverForm.get('numberOfSeats');
      control?.setValue(1);
      expect(control?.hasError('min')).toBeFalsy();
      expect(control?.hasError('max')).toBeFalsy();
    });

    it('should accept numberOfSeats 8 (max boundary)', () => {
      const control = component.driverForm.get('numberOfSeats');
      control?.setValue(8);
      expect(control?.hasError('min')).toBeFalsy();
      expect(control?.hasError('max')).toBeFalsy();
    });

    it('should reject numberOfSeats 0', () => {
      const control = component.driverForm.get('numberOfSeats');
      control?.setValue(0);
      expect(control?.hasError('min')).toBeTruthy();
    });

    it('should reject numberOfSeats 9', () => {
      const control = component.driverForm.get('numberOfSeats');
      control?.setValue(9);
      expect(control?.hasError('max')).toBeTruthy();
    });
  });

  describe('Form Submission - Success', () => {
    beforeEach(() => {
      component.driverForm.patchValue(validDriverFormValues as any);
    });

    it('should call authService.registerDriver with form values on valid submission', () => {
      authService.registerDriver.and.returnValue(of({ message: 'OK', email: 'john@example.com', status: 'created' }));
      component.onSubmit();
      expect(authService.registerDriver).toHaveBeenCalledWith(jasmine.objectContaining(validDriverFormValues));
    });

    it('should set success message on successful submission', () => {
      authService.registerDriver.and.returnValue(of({ message: 'OK', email: 'john@example.com', status: 'created' }));
      component.onSubmit();
      expect(component.successMessage()).toContain('Driver account created');
      expect(component.successMessage()).toContain('activation link');
    });

    it('should clear error message on successful submission', () => {
      component.errorMessage.set('Previous error');
      authService.registerDriver.and.returnValue(of({ message: 'OK', email: 'john@example.com', status: 'created' }));
      component.onSubmit();
      expect(component.errorMessage()).toBeNull();
    });

    it('should reset form after successful submission', () => {
      authService.registerDriver.and.returnValue(of({ message: 'OK', email: 'john@example.com', status: 'created' }));
      component.onSubmit();
      expect(component.driverForm.get('firstName')?.value).toBe('');
      expect(component.driverForm.get('email')?.value).toBe('');
      expect(component.driverForm.get('vehicleModel')?.value).toBe('');
      expect(component.driverForm.get('vehicleType')?.value).toBe('STANDARD');
      expect(component.driverForm.get('numberOfSeats')?.value).toBe(4);
      expect(component.driverForm.get('babyFriendly')?.value).toBe(false);
      expect(component.driverForm.get('petFriendly')?.value).toBe(false);
    });

    it('should set isSubmitting to false after success', () => {
      authService.registerDriver.and.returnValue(of({ message: 'OK', email: 'john@example.com', status: 'created' }));
      component.onSubmit();
      expect(component.isSubmitting()).toBe(false);
    });
  });

  describe('Form Submission - Error', () => {
    beforeEach(() => {
      component.driverForm.patchValue(validDriverFormValues as any);
    });

    it('should set error message on registration failure', () => {
      authService.registerDriver.and.returnValue(throwError(() => new Error('Server error')));
      component.onSubmit();
      expect(component.errorMessage()).toBe('Server error');
    });

    it('should clear success message on error', () => {
      component.successMessage.set('Previous success');
      authService.registerDriver.and.returnValue(throwError(() => new Error('Server error')));
      component.onSubmit();
      expect(component.successMessage()).toBeNull();
    });

    it('should set isSubmitting to false after error', () => {
      authService.registerDriver.and.returnValue(throwError(() => new Error('Server error')));
      component.onSubmit();
      expect(component.isSubmitting()).toBe(false);
    });

    it('should not call registerDriver when form is invalid', () => {
      component.driverForm.patchValue({ firstName: '' });
      component.onSubmit();
      expect(authService.registerDriver).not.toHaveBeenCalled();
    });

    it('should use fallback error message when err.message is empty', () => {
      authService.registerDriver.and.returnValue(throwError(() => new Error('')));
      component.onSubmit();
      expect(component.errorMessage()).toBe('Failed to create driver account. Please try again.');
    });
  });

  describe('isSubmitting behavior', () => {
    it('should set isSubmitting to true during request and false after completion', () => {
      const subject = new Subject<{ message: string; email: string; status: string }>();
      authService.registerDriver.and.returnValue(subject.asObservable());
      component.driverForm.patchValue(validDriverFormValues as any);
      component.onSubmit();
      expect(component.isSubmitting()).toBe(true);
      subject.next({ message: 'OK', email: 'john@example.com', status: 'created' });
      subject.complete();
      expect(component.isSubmitting()).toBe(false);
    });
  });

  describe('UI/Template Rendering', () => {
    it('should render all form input fields', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('#firstName')).toBeTruthy();
      expect(compiled.querySelector('#lastName')).toBeTruthy();
      expect(compiled.querySelector('#email')).toBeTruthy();
      expect(compiled.querySelector('#address')).toBeTruthy();
      expect(compiled.querySelector('#phoneNumber')).toBeTruthy();
      expect(compiled.querySelector('#vehicleModel')).toBeTruthy();
      expect(compiled.querySelector('#vehicleType')).toBeTruthy();
      expect(compiled.querySelector('#licensePlate')).toBeTruthy();
      expect(compiled.querySelector('#numberOfSeats')).toBeTruthy();
      const checkboxes = compiled.querySelectorAll('input[type="checkbox"]');
      expect(checkboxes.length).toBe(2);
    });

    it('should render submit button with correct text when not submitting', () => {
      const button = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button).toBeTruthy();
      expect(button.textContent?.trim()).toContain('Create Account & Send Invite');
    });

    it('should show Creating... when isSubmitting is true', () => {
      component.driverForm.patchValue(validDriverFormValues as any);
      component.isSubmitting.set(true);
      fixture.detectChanges();
      const button = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button?.textContent?.trim()).toContain('Creating...');
    });

    it('should disable submit button when form is invalid', () => {
      const button = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button?.hasAttribute('disabled')).toBeTruthy();
    });

    it('should enable submit button when form is valid', () => {
      component.driverForm.patchValue(validDriverFormValues as any);
      fixture.detectChanges();
      const button = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button?.hasAttribute('disabled')).toBeFalsy();
    });

    it('should disable submit button when isSubmitting is true', () => {
      component.driverForm.patchValue(validDriverFormValues as any);
      component.isSubmitting.set(true);
      fixture.detectChanges();
      const button = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button?.hasAttribute('disabled')).toBeTruthy();
    });

    it('should display error message when errorMessage signal is set', () => {
      component.errorMessage.set('Test error message');
      fixture.detectChanges();
      const errorDiv = fixture.nativeElement.querySelector('.error-message');
      expect(errorDiv).toBeTruthy();
      expect(errorDiv?.textContent?.trim()).toBe('Test error message');
    });

    it('should display success message when successMessage signal is set', () => {
      component.successMessage.set('Test success message');
      fixture.detectChanges();
      const successDiv = fixture.nativeElement.querySelector('.success-message');
      expect(successDiv).toBeTruthy();
      expect(successDiv?.textContent?.trim()).toBe('Test success message');
    });

    it('should show field error when field is touched and invalid', () => {
      const firstNameControl = component.driverForm.get('firstName');
      firstNameControl?.markAsTouched();
      fixture.detectChanges();
      const fieldError = fixture.nativeElement.querySelector('.field-error');
      expect(fieldError).toBeTruthy();
    });

    it('should apply invalid CSS class to touched invalid fields', () => {
      const firstNameControl = component.driverForm.get('firstName');
      firstNameControl?.markAsTouched();
      fixture.detectChanges();
      const input = fixture.nativeElement.querySelector('#firstName');
      expect(input?.classList.contains('invalid')).toBeTruthy();
    });
  });

  describe('Edge Cases', () => {
    it('should have invalid form after reset following successful submit', () => {
      component.driverForm.patchValue(validDriverFormValues as any);
      expect(component.driverForm.valid).toBeTruthy();
      authService.registerDriver.and.returnValue(of({ message: 'OK', email: 'john@example.com', status: 'created' }));
      component.onSubmit();
      expect(component.driverForm.valid).toBeFalsy();
    });

    it('should handle two valid submissions in sequence', () => {
      authService.registerDriver.and.returnValue(of({ message: 'OK', email: 'john@example.com', status: 'created' }));
      component.driverForm.patchValue(validDriverFormValues as any);
      component.onSubmit();
      component.driverForm.patchValue(validDriverFormValues as any);
      component.onSubmit();
      expect(authService.registerDriver).toHaveBeenCalledTimes(2);
    });
  });

  describe('Vehicle Type Options', () => {
    it('should have vehicleTypes with STANDARD, COMFORT, VAN, LUXURY', () => {
      const values = component.vehicleTypes.map(t => t.value);
      expect(values).toContain('STANDARD');
      expect(values).toContain('COMFORT');
      expect(values).toContain('VAN');
      expect(values).toContain('LUXURY');
    });

    it('should render vehicle type select with options', () => {
      const select = fixture.nativeElement.querySelector('#vehicleType');
      expect(select).toBeTruthy();
      const options = (fixture.nativeElement as HTMLElement).querySelectorAll('#vehicleType option');
      expect(options.length).toBe(4);
    });
  });
});
