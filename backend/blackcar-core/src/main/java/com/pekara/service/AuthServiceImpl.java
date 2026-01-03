package com.pekara.service;

import com.pekara.dto.request.ActivateAccountRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AccountActivationTokenRepository tokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

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
    public AuthResponse activateAccount(ActivateAccountRequest request) {
        AccountActivationToken token = tokenRepository.findByToken(request.getToken())
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
}
