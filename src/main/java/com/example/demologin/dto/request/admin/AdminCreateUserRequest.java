package com.example.demologin.dto.request.admin;

import com.example.demologin.annotation.StrongPassword;
import com.example.demologin.annotation.ValidEmail;
import com.example.demologin.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Password must not be blank")
    @StrongPassword
    private String password;

    @NotBlank(message = "Full name must not be blank")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email must not be blank")
    @ValidEmail
    private String email;

    @NotBlank(message = "Role name must not be blank")
    private String roleName;

    @NotNull(message = "Status must not be null")
    private UserStatus status;

    private Boolean verify;
}
