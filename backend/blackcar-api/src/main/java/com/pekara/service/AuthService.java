package com.pekara.service;

import com.pekara.dto.request.RegisterDriverRequest;
import com.pekara.dto.request.RegisterUserRequest;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.RegisterDriverResponse;
import com.pekara.dto.response.RegisterResponse;

public interface AuthService {
    RegisterResponse registerUser(RegisterUserRequest request);
    RegisterDriverResponse registerDriver(RegisterDriverRequest request);
    AuthResponse activateAccount(String token);
    AuthResponse login(String email, String password);
}
