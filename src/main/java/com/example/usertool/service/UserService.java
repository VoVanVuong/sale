package com.example.usertool.service;

import com.example.usertool.cache.UserCache;
import com.example.usertool.dto.request.UserCreationRequest;
import com.example.usertool.dto.request.UserUpdateRequest;
import com.example.usertool.dto.response.UserResponse;
import com.example.usertool.entity.User;
import com.example.usertool.exception.AppException;
import com.example.usertool.exception.ErrorCode;
import com.example.usertool.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCache userCache;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserCache userCache) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userCache = userCache;
    }

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        seedAuthState(saved.getId());

        return toResponse(saved);
    }

    private void seedAuthState(Long userId) {
        try {
            userCache.initialize(userId);
        } catch (Exception e) {
            // Registration succeeds even if the cache is unavailable; the auth
            // state can be re-seeded later. Do not fail the request.
            log.error("Failed to seed Redis auth state for user {}", userId, e);
        }
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAllByDeletedFalse()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUser(Long id) {
        return toResponse(findActiveUser(id));
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findActiveUser(id);

        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return toResponse(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        User user = findActiveUser(id);
        user.setDeleted(true);
        userRepository.save(user);
    }

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
