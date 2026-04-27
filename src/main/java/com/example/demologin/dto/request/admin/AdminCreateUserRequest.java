package com.example.demologin.dto.request.admin;

import com.example.demologin.annotation.ValidEmail;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserRequest {
    @NotBlank(message = "Email must not be blank")
    @ValidEmail
    private String email;

    @NotBlank(message = "Role name must not be blank")
    private String roleName;

    private String storeId;

    private String kitchenId;
}
