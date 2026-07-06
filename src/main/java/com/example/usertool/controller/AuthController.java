package com.example.usertool.controller;

import com.example.usertool.common.ApiResponse;
import com.example.usertool.dto.LoginRequest;
import com.example.usertool.dto.LoginResponse;
import com.example.usertool.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse result = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Login successful", result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LoginRequest request) {
        authService.logout(request.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}
