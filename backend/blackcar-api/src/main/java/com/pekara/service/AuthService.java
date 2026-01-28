package com.pekara.service;

import com.pekara.dto.request.RegisterDriverRequest;
import com.pekara.dto.request.RegisterUserRequest;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.RegisterDriverResponse;
import com.pekara.dto.response.RegisterResponse;
import com.pekara.dto.response.ActivationInfoResponse;

public interface AuthService {
    RegisterResponse registerUser(RegisterUserRequest request);
    RegisterDriverResponse registerDriver(RegisterDriverRequest request);
    ActivationInfoResponse getActivationInfo(String token);
    AuthResponse activateAccount(String token);
    AuthResponse setNewPassword(String token, String newPassword);
    AuthResponse login(String email, String password);
    AuthResponse getCurrentUser(String email);
}
