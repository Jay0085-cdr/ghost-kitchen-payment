package com.ghostkitchen.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "organizationName is required")
        @Size(max = 200, message = "organizationName must be at most 200 characters")
        String organizationName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be at least 8 characters")
        String password
) {
}
