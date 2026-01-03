package com.pekara.service;

import com.pekara.dto.request.RegisterUserRequest;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.RegisterResponse;
import com.pekara.exception.DuplicateResourceException;
import com.pekara.exception.InvalidTokenException;
import com.pekara.model.AccountActivationToken;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.AccountActivationTokenRepository;
import com.pekara.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountActivationTokenRepository tokenRepository;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterUserRequest registerRequest;
    private User user;
    private AccountActivationToken activationToken;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterUserRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+381641234567")
                .address("Test Address 123")
                .build();

        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+381641234567")
                .address("Test Address 123")
                .role(UserRole.PASSENGER)
                .isActive(false)
                .totalRides(0)
                .build();

        activationToken = AccountActivationToken.builder()
                .id(1L)
                .token("test-token-123")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .activatedAt(null)
                .build();
    }

    // ========== REGISTER USER TESTS ==========

    @Test
    @DisplayName("registerUser - should successfully register new user")
    void registerUser_Success() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(AccountActivationToken.class))).thenReturn(activationToken);
        doNothing().when(mailService).sendActivationEmail(anyString(), anyString());

        // When
        RegisterResponse response = authService.registerUser(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Registration successful. Please check your email to activate your account.");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(AccountActivationToken.class));
        verify(mailService).sendActivationEmail(eq("test@example.com"), anyString());
    }

    @Test
    @DisplayName("registerUser - should throw DuplicateResourceException when email exists")
    void registerUser_EmailExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(mailService, never()).sendActivationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("registerUser - should throw DuplicateResourceException when username exists")
    void registerUser_UsernameExists_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.registerUser(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists");

        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(userRepository, never()).save(any(User.class));
        verify(mailService, never()).sendActivationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("registerUser - should create user with correct role and inactive status")
    void registerUser_CreatesUserWithCorrectAttributes() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(AccountActivationToken.class))).thenReturn(activationToken);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        authService.registerUser(registerRequest);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getRole()).isEqualTo(UserRole.PASSENGER);
        assertThat(savedUser.getIsActive()).isFalse();
        assertThat(savedUser.getTotalRides()).isEqualTo(0);
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
    }

    // ========== ACTIVATE ACCOUNT TESTS ==========

    @Test
    @DisplayName("activateAccount - should successfully activate account")
    void activateAccount_Success() {
        // Given
        String tokenValue = "test-token-123";
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(activationToken));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(AccountActivationToken.class))).thenReturn(activationToken);

        // When
        AuthResponse response = authService.activateAccount(tokenValue);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Account activated successfully. You can now log in.");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo("PASSENGER");

        verify(tokenRepository).findByToken(tokenValue);
        verify(userRepository).save(user);
        verify(tokenRepository).save(activationToken);
        assertThat(user.getIsActive()).isTrue();
        assertThat(activationToken.getActivatedAt()).isNotNull();
    }

    @Test
    @DisplayName("activateAccount - should throw InvalidTokenException when token not found")
    void activateAccount_TokenNotFound_ThrowsException() {
        // Given
        String tokenValue = "invalid-token";
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.activateAccount(tokenValue))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid activation token");

        verify(tokenRepository).findByToken(tokenValue);
        verify(userRepository, never()).save(any(User.class));
        verify(tokenRepository, never()).save(any(AccountActivationToken.class));
    }

    @Test
    @DisplayName("activateAccount - should throw InvalidTokenException when already activated")
    void activateAccount_AlreadyActivated_ThrowsException() {
        // Given
        String tokenValue = "test-token-123";
        activationToken.setActivatedAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(activationToken));

        // When & Then
        assertThatThrownBy(() -> authService.activateAccount(tokenValue))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Account already activated");

        verify(tokenRepository).findByToken(tokenValue);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("activateAccount - should throw InvalidTokenException when token expired")
    void activateAccount_TokenExpired_ThrowsException() {
        // Given
        String tokenValue = "test-token-123";
        activationToken.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(activationToken));

        // When & Then
        assertThatThrownBy(() -> authService.activateAccount(tokenValue))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Activation token expired");

        verify(tokenRepository).findByToken(tokenValue);
        verify(userRepository, never()).save(any(User.class));
    }

    // ========== LOGIN TESTS ==========

    @Test
    @DisplayName("login - should successfully login with valid credentials")
    void login_Success() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        String jwtToken = "jwt-token-123";

        user.setIsActive(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(email, "PASSENGER")).thenReturn(jwtToken);

        // When
        AuthResponse response = authService.login(email, password);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Login successful");
        assertThat(response.getToken()).isEqualTo(jwtToken);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRole()).isEqualTo("PASSENGER");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, user.getPassword());
        verify(jwtService).generateToken(email, "PASSENGER");
    }

    @Test
    @DisplayName("login - should throw BadCredentialsException when user not found")
    void login_UserNotFound_ThrowsException() {
        // Given
        String email = "nonexistent@example.com";
        String password = "password123";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("login - should throw BadCredentialsException when password is wrong")
    void login_WrongPassword_ThrowsException() {
        // Given
        String email = "test@example.com";
        String password = "wrongpassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, user.getPassword());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("login - should throw BadCredentialsException when account not activated")
    void login_AccountNotActivated_ThrowsException() {
        // Given
        String email = "test@example.com";
        String password = "password123";

        user.setIsActive(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPassword())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Account is not activated. Please check your email.");

        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, user.getPassword());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }
}
