package vn.lnt.saas_customer_communication_platform.feature.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.lnt.saas_customer_communication_platform.feature.auth.dto.*;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response);
    AuthResponse refreshToken(String refreshTokenInput, HttpServletRequest request, HttpServletResponse response);
    void logout(String refreshTokenInput, HttpServletRequest request, HttpServletResponse response);
    UserResponse getMe(String currentUserEmail);
}
