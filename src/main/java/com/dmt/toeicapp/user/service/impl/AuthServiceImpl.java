package com.dmt.toeicapp.user.service.impl;

import com.dmt.toeicapp.common.exception.AppException;
import com.dmt.toeicapp.common.security.JwtUtil;
import com.dmt.toeicapp.user.dto.AuthResponse;
import com.dmt.toeicapp.user.dto.LoginRequest;
import com.dmt.toeicapp.user.dto.RegisterRequest;
import com.dmt.toeicapp.user.entity.User;
import com.dmt.toeicapp.user.repository.UserRepository;
import com.dmt.toeicapp.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtUtil             jwtUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Kiểm tra trùng email / username
        if (userRepository.existsByEmail(request.email())) {
            throw AppException.conflict("Email đã được sử dụng", "EMAIL_ALREADY_EXISTS");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw AppException.conflict("Username đã được sử dụng", "USERNAME_ALREADY_EXISTS");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .build();

        userRepository.save(user);
        log.info("User mới đăng ký: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> AppException.badRequest(
                        "Email hoặc mật khẩu không đúng", "INVALID_CREDENTIALS"));

        if (!user.isActive()) {
            throw AppException.forbidden("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AppException.badRequest("Email hoặc mật khẩu không đúng", "INVALID_CREDENTIALS");
        }

        log.info("User đăng nhập: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        // Kiểm tra token hợp lệ và đúng type
        if (!jwtUtil.isValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw AppException.badRequest("Refresh token không hợp lệ", "TOKEN_INVALID");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);

        // Kiểm tra token có trong Redis không (chưa bị revoke)
        String redisKey       = REFRESH_TOKEN_PREFIX + userId;
        String storedToken    = redisTemplate.opsForValue().get(redisKey);

        if (!refreshToken.equals(storedToken)) {
            throw AppException.badRequest("Refresh token đã hết hạn hoặc đã bị thu hồi", "TOKEN_EXPIRED");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy user"));

        if (!user.isActive()) {
            throw AppException.forbidden("Tài khoản đã bị vô hiệu hóa");
        }

        return buildAuthResponse(user);
    }

    @Override
    public void logout(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)) return;

        Long   userId   = jwtUtil.extractUserId(refreshToken);
        String redisKey = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(redisKey);
        log.info("User logout, revoked refresh token userId={}", userId);
    }

    // ── Private helpers ───────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Lưu refresh token vào Redis với TTL khớp với token expiry
        long ttlMs     = jwtUtil.getAccessTokenTtlMs();
        long ttlSec    = ttlMs / 1000;
        String redisKey = REFRESH_TOKEN_PREFIX + user.getId();

        // Lưu refresh token (không phải access token) vào Redis
        // TTL lấy từ refresh token config — hardcode tạm 7 ngày ở đây
        // TODO: tách riêng getRefreshTokenTtlMs() trong JwtUtil nếu cần
        redisTemplate.opsForValue().set(
                redisKey, refreshToken, jwtUtil.getRefreshTokenTtlMs(), TimeUnit.DAYS
        );

        return AuthResponse.of(
                accessToken,
                refreshToken,
                ttlSec,
                new AuthResponse.UserInfo(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().name()
                )
        );
    }
}