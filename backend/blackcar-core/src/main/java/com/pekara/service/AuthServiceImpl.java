package com.pekara.service;

import com.pekara.dto.request.RegisterDriverRequest;
import com.pekara.dto.request.RegisterUserRequest;
import com.pekara.dto.response.ActivationInfoResponse;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.RegisterDriverResponse;
import com.pekara.dto.response.RegisterResponse;
import com.pekara.exception.DuplicateResourceException;
import com.pekara.exception.InvalidTokenException;
import com.pekara.model.AccountActivationToken;
import com.pekara.model.Driver;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.AccountActivationTokenRepository;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final AccountActivationTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public RegisterResponse registerUser(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(UserRole.PASSENGER)
                .isActive(false)
                .totalRides(0)
                .build();

        User savedUser = userRepository.save(user);

        String tokenValue = UUID.randomUUID().toString();
        AccountActivationToken token = AccountActivationToken.builder()
                .token(tokenValue)
                .user(savedUser)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(token);

        mailService.sendActivationEmail(savedUser.getEmail(), tokenValue);

        return RegisterResponse.builder()
                .message("Registration successful. Please check your email to activate your account.")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .build();
    }

    @Override
    @Transactional
    public RegisterDriverResponse registerDriver(RegisterDriverRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        if (driverRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new DuplicateResourceException("Vehicle with this license plate already registered");
        }

        // Generate a temporary password - driver will set their own on first login
        String tempPassword = UUID.randomUUID().toString().substring(0, 12);

        Driver driver = Driver.builder()
                .email(request.getEmail())
                .username(generateUsernameFromEmail(request.getEmail()))
                .password(passwordEncoder.encode(tempPassword))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(UserRole.DRIVER)
                .isActive(false)
                .totalRides(0)
            .licenseNumber("LIC-" + request.getLicensePlate())
            .vehicleModel(request.getVehicleModel())
            .vehicleType(request.getVehicleType())
            .licensePlate(request.getLicensePlate())
            .numberOfSeats(request.getNumberOfSeats())
            .babyFriendly(request.getBabyFriendly())
            .petFriendly(request.getPetFriendly())
                .build();

        Driver savedDriver = driverRepository.save(driver);

        String tokenValue = UUID.randomUUID().toString();
        AccountActivationToken token = AccountActivationToken.builder()
                .token(tokenValue)
                .user(savedDriver)
            .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(token);

        mailService.sendDriverActivationEmail(savedDriver.getEmail(), tokenValue, savedDriver.getFirstName());

        return RegisterDriverResponse.builder()
                .message("Driver registration successful. An activation link has been sent to the driver's email.")
                .userId(savedDriver.getId())
                .email(savedDriver.getEmail())
                .status("PENDING_ACTIVATION")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivationInfoResponse getActivationInfo(String tokenValue) {
        AccountActivationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid activation token"));

        if (token.isActivated()) {
            throw new InvalidTokenException("Activation token already used");
        }

        if (token.isExpired()) {
            throw new InvalidTokenException("Activation token expired");
        }

        User user = token.getUser();
        boolean requiresPassword = user.getRole() == UserRole.DRIVER;

        return new ActivationInfoResponse(
                requiresPassword,
                user.getRole().name(),
                user.getEmail()
        );
    }

    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter++;
        }
        return username;
    }

    @Override
    @Transactional
    public AuthResponse activateAccount(String tokenValue) {
        AccountActivationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid activation token"));

        if (token.isActivated()) {
            throw new InvalidTokenException("Account already activated");
        }

        if (token.isExpired()) {
            throw new InvalidTokenException("Activation token expired");
        }

        User user = token.getUser();
        user.setIsActive(true);
        userRepository.save(user);

        token.setActivatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        return AuthResponse.builder()
                .message("Account activated successfully. You can now log in.")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse setNewPassword(String tokenValue, String newPassword) {
        AccountActivationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidTokenException("Invalid activation token"));

        if (token.isActivated()) {
            throw new InvalidTokenException("Activation token already used");
        }

        if (token.isExpired()) {
            throw new InvalidTokenException("Activation token expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setIsActive(true);
        userRepository.save(user);

        token.setActivatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        return AuthResponse.builder()
                .message("Password set successfully. You can now log in.")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new BadCredentialsException("Account is not activated. Please check your email.");
        }

        String jwtToken = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .message("Login successful")
                .token(jwtToken)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
