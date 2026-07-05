package com.example.usertool.controller;

import com.example.usertool.dto.response.UserResponse;
import com.example.usertool.enums.Role;
import com.example.usertool.exception.AppException;
import com.example.usertool.exception.ErrorCode;
import com.example.usertool.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final UserResponse sample =
            new UserResponse(1L, "alice", "alice@example.com", Role.USER);

    @Test
    void createUser_valid_returnsSuccess() throws Exception {
        when(userService.createUser(any())).thenReturn(sample);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("User created"))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.username").value("alice"))
                .andExpect(jsonPath("$.result.role").value("USER"));
    }

    @Test
    void createUser_blankUsername_returnsValidationError() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"alice@example.com","password":"secret123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1002));
    }

    @Test
    void createUser_duplicate_returnsConflict() throws Exception {
        when(userService.createUser(any())).thenThrow(new AppException(ErrorCode.USER_EXISTED));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"secret123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void getUsers_returnsList() throws Exception {
        when(userService.getUsers()).thenReturn(List.of(sample));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].username").value("alice"));
    }

    @Test
    void getUser_found_returnsUser() throws Exception {
        when(userService.getUser(1L)).thenReturn(sample);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.email").value("alice@example.com"));
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        when(userService.getUser(99L)).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1003));
    }

    @Test
    void updateUser_returnsUpdated() throws Exception {
        UserResponse updated = new UserResponse(1L, "alice", "new@example.com", Role.USER);
        when(userService.updateUser(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated"))
                .andExpect(jsonPath("$.result.email").value("new@example.com"));
    }

    @Test
    void deleteUser_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("User deleted"));

        verify(userService).deleteUser(1L);
    }
}
