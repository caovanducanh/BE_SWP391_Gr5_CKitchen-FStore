package com.example.demologin.dto.request.admin;

import com.example.demologin.annotation.ValidEmail;
import com.example.demologin.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRequest {
    private String fullName;

    @ValidEmail
    private String email;

    private String roleName;

    private UserStatus status;

    private Boolean verify;

    private Boolean locked;

    private String storeId;

    private String kitchenId;
}
