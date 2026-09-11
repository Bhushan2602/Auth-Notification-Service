package com.authplatform.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class RegisterRequest {
        @NotBlank private String fullName;
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Email private String email;
        @NotBlank private String password;
        private String deviceLabel;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank private String refreshToken;
    }

    @Data
    public static class ForgotRequest {
        @NotBlank @Email private String email;
    }

    @Data
    public static class ResetRequest {
        @NotBlank private String token;
        @NotBlank @Size(min = 6) private String newPassword;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank @Email private String email;
        @NotBlank private String oldPassword;
        @NotBlank @Size(min = 6) private String newPassword;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank @Email private String email;
        @NotBlank private String fullName;
    }

    @Data
    public static class RoleRequest {
        @NotBlank @Email private String email;
        @NotBlank private String role;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String email;
        private String fullName;
        private String role;

        public AuthResponse(String accessToken, String refreshToken, String email, String fullName, String role) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
        }
    }
}
