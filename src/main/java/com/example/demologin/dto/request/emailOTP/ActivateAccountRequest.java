package com.example.demologin.dto.request.emailOTP;

import com.example.demologin.annotation.StrongPassword;
import com.example.demologin.annotation.ValidEmail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivateAccountRequest {
    @NotBlank(message = "Email cannot be blank")
    @ValidEmail
    private String email;

    @NotBlank(message = "OTP cannot be blank")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otp;

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @StrongPassword
    private String password;

    @NotBlank(message = "Confirm password must not be blank")
    private String confirmPassword;

    @NotBlank(message = "Full name must not be blank")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
}
