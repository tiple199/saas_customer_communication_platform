package vn.lnt.saas_customer_communication_platform.feature.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.lnt.saas_customer_communication_platform.dto.ApiResponse;
import vn.lnt.saas_customer_communication_platform.feature.auth.dto.*;
import vn.lnt.saas_customer_communication_platform.feature.auth.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký tài khoản thành công", userResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, httpRequest, response);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        String tokenInput = requestBody != null ? requestBody.getRefreshToken() : null;
        AuthResponse authResponse = authService.refreshToken(tokenInput, request, response);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        String tokenInput = requestBody != null ? requestBody.getRefreshToken() : null;
        authService.logout(tokenInput, request, response);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal Jwt jwt) {
        String currentUserEmail = jwt != null ? jwt.getSubject() : null;
        UserResponse userResponse = authService.getMe(currentUserEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", userResponse));
    }
}
