package com.example.demologin.service;

import com.example.demologin.dto.request.admin.AdminCreateUserRequest;
import com.example.demologin.dto.request.admin.AdminUpdateUserRequest;
import com.example.demologin.dto.response.MemberResponse;
import com.example.demologin.enums.UserStatus;
import org.springframework.data.domain.Page;

public interface UserService {
    Page<MemberResponse> getAllUsers(String roleName, UserStatus status, int page, int size);

    MemberResponse getUserById(Long userId);

    MemberResponse createUser(AdminCreateUserRequest request);

    MemberResponse updateUser(Long userId, AdminUpdateUserRequest request);

    void deleteUser(Long userId);
} 
