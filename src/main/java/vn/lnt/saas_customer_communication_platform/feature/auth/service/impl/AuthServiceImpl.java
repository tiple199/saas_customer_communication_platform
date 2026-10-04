package vn.lnt.saas_customer_communication_platform.feature.auth.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.lnt.saas_customer_communication_platform.exception.DuplicateResourceException;
import vn.lnt.saas_customer_communication_platform.exception.InvalidOperationException;
import vn.lnt.saas_customer_communication_platform.exception.InvalidTokenException;
import vn.lnt.saas_customer_communication_platform.exception.ResourceNotFoundException;
import vn.lnt.saas_customer_communication_platform.feature.auth.dto.*;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.RefreshToken;
import vn.lnt.saas_customer_communication_platform.feature.auth.entity.User;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.RefreshTokenRepository;
import vn.lnt.saas_customer_communication_platform.feature.auth.repository.UserRepository;
import vn.lnt.saas_customer_communication_platform.feature.auth.service.AuthService;
import vn.lnt.saas_customer_communication_platform.feature.auth.service.JwtTokenProvider;
import vn.lnt.saas_customer_communication_platform.feature.auth.util.CookieHelper;
import vn.lnt.saas_customer_communication_platform.feature.auth.util.TokenHashUtils;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieHelper cookieHelper;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           CookieHelper cookieHelper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieHelper = cookieHelper;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getDeviceInfo(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.getPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidOperationException("Mật khẩu xác nhận không trùng khớp");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                "ROLE_USER"
        );

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }

        String accessTokenStr = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user);
        String refreshTokenHash = TokenHashUtils.hashToken(refreshTokenStr);

        RefreshToken refreshToken = new RefreshToken(
                refreshTokenHash,
                user,
                jwtTokenProvider.getRefreshTokenExpiryDate(),
                getClientIp(httpRequest),
                getDeviceInfo(httpRequest)
        );
        refreshTokenRepository.save(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createAccessTokenCookie(accessTokenStr).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createRefreshTokenCookie(refreshTokenStr).toString());

        return new AuthResponse(accessTokenStr, refreshTokenStr, UserResponse.fromEntity(user));
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenInput, HttpServletRequest request, HttpServletResponse response) {
        String tokenToValidate = refreshTokenInput;

        if (!StringUtils.hasText(tokenToValidate)) {
            tokenToValidate = cookieHelper.getCookieValue(request, CookieHelper.REFRESH_TOKEN_COOKIE_NAME)
                    .orElse(null);
        }

        if (!StringUtils.hasText(tokenToValidate)) {
            throw new InvalidTokenException("Refresh token is missing");
        }

        Jwt jwt;
        try {
            jwt = jwtTokenProvider.decodeToken(tokenToValidate);
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        String tokenType = jwt.getClaimAsString("type");
        if (!"REFRESH".equals(tokenType)) {
            throw new InvalidTokenException("Invalid token type for refresh");
        }

        String hashedTokenToValidate = TokenHashUtils.hashToken(tokenToValidate);

        RefreshToken existingToken = refreshTokenRepository.findByToken(hashedTokenToValidate)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (existingToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (existingToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = existingToken.getUser();

        String newAccessTokenStr = jwtTokenProvider.generateAccessToken(user);
        String newRefreshTokenStr = jwtTokenProvider.generateRefreshToken(user);
        String newRefreshTokenHash = TokenHashUtils.hashToken(newRefreshTokenStr);

        existingToken.setRevoked(true);
        existingToken.setReplacedByToken(newRefreshTokenHash);
        refreshTokenRepository.save(existingToken);

        RefreshToken newRefreshTokenEntity = new RefreshToken(
                newRefreshTokenHash,
                user,
                jwtTokenProvider.getRefreshTokenExpiryDate(),
                getClientIp(request),
                getDeviceInfo(request)
        );
        refreshTokenRepository.save(newRefreshTokenEntity);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createAccessTokenCookie(newAccessTokenStr).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.createRefreshTokenCookie(newRefreshTokenStr).toString());

        return new AuthResponse(newAccessTokenStr, newRefreshTokenStr, UserResponse.fromEntity(user));
    }

    @Override
    @Transactional
    public void logout(String refreshTokenInput, HttpServletRequest request, HttpServletResponse response) {
        String tokenToRevoke = refreshTokenInput;

        if (!StringUtils.hasText(tokenToRevoke)) {
            tokenToRevoke = cookieHelper.getCookieValue(request, CookieHelper.REFRESH_TOKEN_COOKIE_NAME)
                    .orElse(null);
        }

        if (StringUtils.hasText(tokenToRevoke)) {
            String hashedTokenToRevoke = TokenHashUtils.hashToken(tokenToRevoke);
            refreshTokenRepository.findByToken(hashedTokenToRevoke).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.cleanAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.cleanRefreshTokenCookie().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(String currentUserEmail) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentUserEmail));

        return UserResponse.fromEntity(user);
    }
}
