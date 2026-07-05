package com.example.usertool.dto.response;

import com.example.usertool.enums.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role
) {
}
