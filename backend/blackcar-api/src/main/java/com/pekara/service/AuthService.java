package com.pekara.service;

import com.pekara.dto.request.ActivateAccountRequest;
import com.pekara.dto.request.RegisterUserRequest;
import com.pekara.dto.response.AuthResponse;
import com.pekara.dto.response.RegisterResponse;

public interface AuthService {
    RegisterResponse registerUser(RegisterUserRequest request);
    AuthResponse activateAccount(ActivateAccountRequest request);
}
