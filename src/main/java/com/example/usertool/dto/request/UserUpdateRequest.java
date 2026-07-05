package com.example.usertool.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Fields are optional; only non-null values are applied to the target user.
 * When present, they must still satisfy the constraints below.
 */
public record UserUpdateRequest(

        @Email(message = "Email is invalid")
        String email,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {
}
