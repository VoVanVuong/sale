package com.example.usertool.service;

import com.example.usertool.cache.UserCache;
import com.example.usertool.dto.request.UserCreationRequest;
import com.example.usertool.dto.request.UserUpdateRequest;
import com.example.usertool.dto.response.UserResponse;
import com.example.usertool.entity.User;
import com.example.usertool.enums.Role;
import com.example.usertool.exception.AppException;
import com.example.usertool.exception.ErrorCode;
import com.example.usertool.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserCache userCache;

    @InjectMocks
    private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("alice");
        existingUser.setEmail("alice@example.com");
        existingUser.setPassword("hashed-old");
        existingUser.setRole(Role.USER);
    }

    @Test
    void createUser_success_hashesPasswordAndSaves() {
        UserCreationRequest request =
                new UserCreationRequest("bob", "bob@example.com", "secret123");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertEquals(2L, response.id());
        assertEquals("bob", response.username());
        assertEquals("bob@example.com", response.email());
        assertEquals(Role.USER, response.role());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("hashed-secret", saved.getValue().getPassword());

        // Auth state is seeded in Redis for the newly created user.
        verify(userCache).initialize(2L);
    }

    @Test
    void createUser_redisFailure_stillCreatesUser() {
        UserCreationRequest request =
                new UserCreationRequest("bob", "bob@example.com", "secret123");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });
        doThrow(new RuntimeException("redis down")).when(userCache).initialize(anyLong());

        UserResponse response = userService.createUser(request);

        // Registration succeeds despite the cache failure.
        assertEquals(2L, response.id());
        assertEquals("bob", response.username());
    }

    @Test
    void createUser_duplicateUsername_throwsUserExisted() {
        UserCreationRequest request =
                new UserCreationRequest("alice", "alice@example.com", "secret123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.createUser(request));

        assertEquals(ErrorCode.USER_EXISTED, ex.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userCache, never()).initialize(anyLong());
    }

    @Test
    void getUsers_returnsMappedActiveUsers() {
        when(userRepository.findAllByDeletedFalse()).thenReturn(List.of(existingUser));

        List<UserResponse> result = userService.getUsers();

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).username());
    }

    @Test
    void getUser_found_returnsResponse() {
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingUser));

        UserResponse response = userService.getUser(1L);

        assertEquals(1L, response.id());
        assertEquals("alice", response.username());
    }

    @Test
    void getUser_notFound_throwsUserNotFound() {
        when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> userService.getUser(99L));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateUser_appliesEmailAndHashesNewPassword() {
        UserUpdateRequest request = new UserUpdateRequest("new@example.com", "newpass123");
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newpass123")).thenReturn("hashed-new");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(1L, request);

        assertEquals("new@example.com", response.email());
        assertEquals("hashed-new", existingUser.getPassword());
    }

    @Test
    void updateUser_nullFields_leaveExistingValuesUnchanged() {
        UserUpdateRequest request = new UserUpdateRequest(null, null);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(1L, request);

        assertEquals("alice@example.com", response.email());
        assertEquals("hashed-old", existingUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_notFound_throwsUserNotFound() {
        UserUpdateRequest request = new UserUpdateRequest("x@example.com", null);
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> userService.updateUser(1L, request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_marksUserAsDeleted() {
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(existingUser.isDeleted());

        userService.deleteUser(1L);

        assertTrue(existingUser.isDeleted());
        verify(userRepository).save(existingUser);
    }

    @Test
    void deleteUser_notFound_throwsUserNotFound() {
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> userService.deleteUser(1L));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }
}
