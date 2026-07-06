package com.example.usertool.service;

import com.example.usertool.dto.LoginRequest;
import com.example.usertool.dto.LoginResponse;
import com.example.usertool.entity.Users;
import com.example.usertool.exception.AppException;
import com.example.usertool.exception.ErrorCode;
import com.example.usertool.repository.UserRepository;
import com.example.usertool.security.JwtTokenProvider;
import com.example.usertool.security.RedisTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTokenService redisTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       RedisTokenService redisTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTokenService = redisTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        Users user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        redisTokenService.saveToken(user.getUsername(), token);

        return new LoginResponse(token);
    }

    public void logout(String username) {
        redisTokenService.deleteToken(username);
    }
}
