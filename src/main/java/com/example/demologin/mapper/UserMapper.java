package com.example.demologin.mapper;

import org.springframework.stereotype.Component;

import com.example.demologin.dto.request.user.UserRequest;
import com.example.demologin.dto.response.LoginResponse;
import com.example.demologin.dto.response.MemberResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.UserStatus;

@Component
public class UserMapper {
    public MemberResponse toUserResponse(User user) {
    String roleName = user.getRole() != null ? user.getRole().getName() : "";
        return new MemberResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                null,
                user.getFullName(),
                null,
                null,
                null,
                user.getStatus(),
                roleName
        );
    }
    public static User toEntity(UserRequest userRequest) {
        User user = new User(
                userRequest.getUsername(),
                null, // password will be set elsewhere
                userRequest.getFullname(),
                userRequest.getEmail()
        );

        user.setStatus(UserStatus.ACTIVE); // Default status
        
        return user;
    }

    // Convert User -> LoginResponse (cho login) - chỉ trả về token và refreshToken
    public static LoginResponse toLoginResponse(User user, String token, String refreshToken) {
        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }
}
