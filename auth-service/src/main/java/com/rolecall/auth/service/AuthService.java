package com.rolecall.auth.service;

import com.rolecall.auth.dto.AuthResponse;
import com.rolecall.auth.dto.LoginRequest;
import com.rolecall.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);
}
