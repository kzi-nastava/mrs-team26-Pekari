import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterComponent, passwordMatchValidator } from './register.component';
import { AuthService } from '../../../core/services/auth.service';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Directive, Input, HostListener } from '@angular/core';

// Mock RouterLink directive
@Directive({
  selector: '[routerLink]',
  standalone: true
})
class MockRouterLinkDirective {
  @Input() routerLink: string | any[] = [];
  @HostListener('click') onClick() {
    return false;
  }
}

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['register']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, ReactiveFormsModule, MockRouterLinkDirective],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    })
    .overrideComponent(RegisterComponent, {
      remove: { imports: [RouterLink] },
      add: { imports: [MockRouterLinkDirective] }
    })
    .compileComponents();

    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // 1. Component Initialization Tests
  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize form with all required fields', () => {
      expect(component.registerForm.contains('firstName')).toBeTruthy();
      expect(component.registerForm.contains('lastName')).toBeTruthy();
      expect(component.registerForm.contains('email')).toBeTruthy();
      expect(component.registerForm.contains('address')).toBeTruthy();
      expect(component.registerForm.contains('phoneNumber')).toBeTruthy();
      expect(component.registerForm.contains('password')).toBeTruthy();
      expect(component.registerForm.contains('confirmPassword')).toBeTruthy();
    });

    it('should initialize all form controls with empty values', () => {
      expect(component.registerForm.get('firstName')?.value).toBe('');
      expect(component.registerForm.get('lastName')?.value).toBe('');
      expect(component.registerForm.get('email')?.value).toBe('');
      expect(component.registerForm.get('address')?.value).toBe('');
      expect(component.registerForm.get('phoneNumber')?.value).toBe('');
      expect(component.registerForm.get('password')?.value).toBe('');
      expect(component.registerForm.get('confirmPassword')?.value).toBe('');
    });

    it('should have invalid form initially', () => {
      expect(component.registerForm.valid).toBeFalsy();
    });

    it('should initialize error and success message signals as null', () => {
      expect(component.errorMessage()).toBeNull();
      expect(component.successMessage()).toBeNull();
    });
  });

  // 2. Required Field Validations
  describe('Required Field Validations', () => {
    it('should validate firstName as required', () => {
      const control = component.registerForm.get('firstName');
      expect(control?.hasError('required')).toBeTruthy();

      control?.setValue('John');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate lastName as required', () => {
      const control = component.registerForm.get('lastName');
      expect(control?.hasError('required')).toBeTruthy();

      control?.setValue('Doe');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate email as required', () => {
      const control = component.registerForm.get('email');
      expect(control?.hasError('required')).toBeTruthy();

      control?.setValue('test@example.com');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate address as required', () => {
      const control = component.registerForm.get('address');
      expect(control?.hasError('required')).toBeTruthy();

      control?.setValue('123 Main St');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate phoneNumber as required', () => {
      const control = component.registerForm.get('phoneNumber');
      expect(control?.hasError('required')).toBeTruthy();

      control?.setValue('+381 60 123 4567');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate password as required', () => {
      const control = component.registerForm.get('password');
      expect(control?.hasError('required')).toBeTruthy();

      control?.setValue('Password1');
      expect(control?.hasError('required')).toBeFalsy();
    });

    it('should validate confirmPassword as required', () => {
      const control = component.registerForm.get('confirmPassword');
      expect(control?.hasError('required')).toBeTruthy();

      control?.setValue('Password1');
      expect(control?.hasError('required')).toBeFalsy();
    });
  });

  // 3. Email Validation
  describe('Email Validation', () => {
    it('should accept valid email format', () => {
      const control = component.registerForm.get('email');
      control?.setValue('user@example.com');
      expect(control?.hasError('email')).toBeFalsy();
    });

    it('should reject email without @ symbol', () => {
      const control = component.registerForm.get('email');
      control?.setValue('userexample.com');
      expect(control?.hasError('email')).toBeTruthy();
    });

    it('should reject email without domain', () => {
      const control = component.registerForm.get('email');
      control?.setValue('user@');
      expect(control?.hasError('email')).toBeTruthy();
    });

    it('should reject email without username', () => {
      const control = component.registerForm.get('email');
      control?.setValue('@example.com');
      expect(control?.hasError('email')).toBeTruthy();
    });
  });

  // 4. Password Validation
  describe('Password Validation', () => {
    it('should reject password shorter than 8 characters', () => {
      const control = component.registerForm.get('password');
      control?.setValue('Pass1');
      expect(control?.hasError('minlength')).toBeTruthy();
    });

    it('should accept password with 8 or more characters', () => {
      const control = component.registerForm.get('password');
      control?.setValue('Password1');
      expect(control?.hasError('minlength')).toBeFalsy();
    });

    it('should reject password without uppercase letter', () => {
      const control = component.registerForm.get('password');
      control?.setValue('password1');
      expect(control?.hasError('pattern')).toBeTruthy();
    });

    it('should reject password without lowercase letter', () => {
      const control = component.registerForm.get('password');
      control?.setValue('PASSWORD1');
      expect(control?.hasError('pattern')).toBeTruthy();
    });

    it('should reject password without number', () => {
      const control = component.registerForm.get('password');
      control?.setValue('Password');
      expect(control?.hasError('pattern')).toBeTruthy();
    });

    it('should accept valid password with uppercase, lowercase, and number', () => {
      const control = component.registerForm.get('password');
      control?.setValue('Password1');
      expect(control?.hasError('pattern')).toBeFalsy();
      expect(control?.hasError('minlength')).toBeFalsy();
    });
  });

  // 5. Password Match Validation
  describe('Password Match Validation', () => {
    it('should return null when passwords match', () => {
      component.registerForm.patchValue({
        password: 'Password1',
        confirmPassword: 'Password1'
      });
      expect(component.registerForm.hasError('passwordMismatch')).toBeFalsy();
    });

    it('should return passwordMismatch error when passwords do not match', () => {
      component.registerForm.patchValue({
        password: 'Password1',
        confirmPassword: 'Password2'
      });
      expect(component.registerForm.hasError('passwordMismatch')).toBeTruthy();
    });

    it('should validate passwordMatchValidator function directly', () => {
      const fb = new FormBuilder();
      const mockForm = fb.group({
        password: ['Password1'],
        confirmPassword: ['Password1']
      });

      expect(passwordMatchValidator(mockForm)).toBeNull();

      mockForm.patchValue({ confirmPassword: 'Different1' });
      expect(passwordMatchValidator(mockForm)).toEqual({ passwordMismatch: true });
    });
  });

  // 6. Form Submission - Success
  describe('Form Submission - Success', () => {
    beforeEach(() => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        address: '123 Main St',
        phoneNumber: '+381 60 123 4567',
        password: 'Password1',
        confirmPassword: 'Password1'
      });
    });

    it('should call authService.register with form values on valid submission', () => {
      authService.register.and.returnValue(of({ message: 'Success', email: 'john@example.com' }));
      const expectedValues = {
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        address: '123 Main St',
        phoneNumber: '+381 60 123 4567',
        password: 'Password1',
        confirmPassword: 'Password1'
      };
      component.onSubmit();
      expect(authService.register).toHaveBeenCalledWith(expectedValues);
    });

    it('should set success message on successful registration', () => {
      authService.register.and.returnValue(of({ message: 'Success', email: 'john@example.com' }));
      component.onSubmit();
      expect(component.successMessage()).toBe('Activation email sent! Please check your inbox. Link expires in 24h.');
    });

    it('should clear error message on successful submission', () => {
      component.errorMessage.set('Previous error');
      authService.register.and.returnValue(of({ message: 'Success', email: 'john@example.com' }));
      component.onSubmit();
      expect(component.errorMessage()).toBeNull();
    });

    it('should reset form after successful registration', () => {
      authService.register.and.returnValue(of({ message: 'Success', email: 'john@example.com' }));
      component.onSubmit();
      expect(component.registerForm.get('firstName')?.value).toBeNull();
      expect(component.registerForm.get('email')?.value).toBeNull();
    });
  });

  // 7. Form Submission - Error
  describe('Form Submission - Error', () => {
    beforeEach(() => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        address: '123 Main St',
        phoneNumber: '+381 60 123 4567',
        password: 'Password1',
        confirmPassword: 'Password1'
      });
    });

    it('should set error message on registration failure', () => {
      authService.register.and.returnValue(throwError(() => new Error('Registration failed')));
      component.onSubmit();
      expect(component.errorMessage()).toBe('Registration failed. Please try again.');
    });

    it('should clear success message on error', () => {
      component.successMessage.set('Previous success');
      authService.register.and.returnValue(throwError(() => new Error('Registration failed')));
      component.onSubmit();
      expect(component.successMessage()).toBeNull();
    });

    it('should not call authService when form is invalid', () => {
      component.registerForm.patchValue({ firstName: '' });
      component.onSubmit();
      expect(authService.register).not.toHaveBeenCalled();
    });
  });

  // 8. UI/Template Tests
  describe('UI/Template Rendering', () => {
    it('should render all form input fields', () => {
      const compiled = fixture.nativeElement;
      expect(compiled.querySelector('#firstName')).toBeTruthy();
      expect(compiled.querySelector('#lastName')).toBeTruthy();
      expect(compiled.querySelector('#email')).toBeTruthy();
      expect(compiled.querySelector('#address')).toBeTruthy();
      expect(compiled.querySelector('#phoneNumber')).toBeTruthy();
      expect(compiled.querySelector('#password')).toBeTruthy();
      expect(compiled.querySelector('#confirmPassword')).toBeTruthy();
    });

    it('should render submit button', () => {
      const compiled = fixture.nativeElement;
      const button = compiled.querySelector('button[type="submit"]');
      expect(button).toBeTruthy();
      expect(button.textContent.trim()).toBe('Create account');
    });

    it('should disable submit button when form is invalid', () => {
      const button = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button.disabled).toBeTruthy();
    });

    it('should enable submit button when form is valid', () => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        address: '123 Main St',
        phoneNumber: '+381 60 123 4567',
        password: 'Password1',
        confirmPassword: 'Password1'
      });
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button.disabled).toBeFalsy();
    });

    it('should display error message when errorMessage signal is set', () => {
      component.errorMessage.set('Test error message');
      fixture.detectChanges();

      const errorDiv = fixture.nativeElement.querySelector('.error-message');
      expect(errorDiv).toBeTruthy();
      expect(errorDiv.textContent.trim()).toBe('Test error message');
    });

    it('should display success message when successMessage signal is set', () => {
      component.successMessage.set('Test success message');
      fixture.detectChanges();

      const successDiv = fixture.nativeElement.querySelector('.success-message');
      expect(successDiv).toBeTruthy();
      expect(successDiv.textContent.trim()).toBe('Test success message');
    });

    it('should show field error when field is touched and invalid', () => {
      const firstNameControl = component.registerForm.get('firstName');
      firstNameControl?.markAsTouched();
      fixture.detectChanges();

      const fieldError = fixture.nativeElement.querySelector('.field-error');
      expect(fieldError).toBeTruthy();
    });

    it('should apply invalid CSS class to touched invalid fields', () => {
      const firstNameControl = component.registerForm.get('firstName');
      firstNameControl?.markAsTouched();
      fixture.detectChanges();

      const input = fixture.nativeElement.querySelector('#firstName');
      expect(input.classList.contains('invalid')).toBeTruthy();
    });

    it('should render login link with correct routerLink', () => {
      const link = fixture.nativeElement.querySelector('a[routerLink="/login"]');
      expect(link).toBeTruthy();
    });
  });

  // 9. Edge Cases
  describe('Edge Cases', () => {
    it('should maintain form validity after reset', () => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        address: '123 Main St',
        phoneNumber: '+381 60 123 4567',
        password: 'Password1',
        confirmPassword: 'Password1'
      });
      expect(component.registerForm.valid).toBeTruthy();

      component.registerForm.reset();
      expect(component.registerForm.valid).toBeFalsy();
    });

    it('should handle multiple rapid form submissions', () => {
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        address: '123 Main St',
        phoneNumber: '+381 60 123 4567',
        password: 'Password1',
        confirmPassword: 'Password1'
      });

      authService.register.and.returnValue(of({ message: 'Success', email: 'john@example.com' }));
      component.onSubmit();

      // Re-fill the form for second submission since form gets reset after successful submission
      component.registerForm.patchValue({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        address: '123 Main St',
        phoneNumber: '+381 60 123 4567',
        password: 'Password1',
        confirmPassword: 'Password1'
      });
      component.onSubmit();

      expect(authService.register).toHaveBeenCalledTimes(2);
    });
  });
});
