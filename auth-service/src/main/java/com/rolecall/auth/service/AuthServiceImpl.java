package com.rolecall.auth.service;

import com.rolecall.auth.dto.AuthResponse;
import com.rolecall.auth.dto.LoginRequest;
import com.rolecall.auth.dto.RegisterRequest;
import com.rolecall.auth.entity.User;
import com.rolecall.auth.entity.UserRole;
import com.rolecall.auth.event.UserEventProducer;
import com.rolecall.auth.exception.EmailAlreadyExistsException;
import com.rolecall.auth.exception.InvalidCredentialsException;
import com.rolecall.auth.repository.UserRepository;
import com.rolecall.auth.security.jwt.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserEventProducer userEventProducer;

    public AuthServiceImpl(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            JwtTokenService jwtTokenService,
                            RefreshTokenService refreshTokenService,
                            UserEventProducer userEventProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userEventProducer = userEventProducer;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role() != null ? request.role() : UserRole.CANDIDATE)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        userEventProducer.publishUserRegistered(user);

        return issueTokens(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.IssuedRefreshToken next = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(next.userId())
                .orElseThrow(InvalidCredentialsException::new);
        return buildResponse(user, next.rawToken());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResponse issueTokens(User user) {
        RefreshTokenService.IssuedRefreshToken issued = refreshTokenService.issueFor(user.getId());
        return buildResponse(user, issued.rawToken());
    }

    private AuthResponse buildResponse(User user, String rawRefreshToken) {
        String accessToken = jwtTokenService.issueAccessToken(user);
        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtTokenService.accessTokenTtlSeconds(),
                user.getId(),
                user.getEmail(),
                user.getRole().name());
    }
}
