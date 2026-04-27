package com.example.demologin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.AuthenticatedEndpoint;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.admin.AdminCreateUserRequest;
import com.example.demologin.dto.request.admin.AdminUpdateUserRequest;
import com.example.demologin.dto.response.UserResponse;
import com.example.demologin.entity.User;
import com.example.demologin.enums.UserStatus;
import com.example.demologin.service.UserService;
import com.example.demologin.utils.AccountUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@Tag(name = "User Management", description = "APIs for managing users (admin only)")
public class UserController {

    private final UserService userService;
    private final AccountUtils accountUtils;

    @GetMapping
    @PageResponse
    @ApiResponse(message = "Users retrieved successfully")
    @SecuredEndpoint("USER_MANAGE")
    @Operation(summary = "Get all users (paginated)",
            description = "Retrieve paginated list of all users in the system (admin only)")
    public Object getAllUsers(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.getAllUsers(roleName, status, page, size);
    }

    @GetMapping("/{id}")
    @ApiResponse(message = "User retrieved successfully")
    @SecuredEndpoint("USER_MANAGE")
    @Operation(summary = "Get user by id", description = "Retrieve a specific user by id")
    public Object getUserById(@Parameter(description = "User ID") @PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @ApiResponse(message = "User created successfully")
    @SecuredEndpoint("USER_MANAGE")
    @Operation(summary = "Create user", description = "Create a new user account with role and status")
    public Object createUser(@RequestBody @Valid AdminCreateUserRequest request) {
        return userService.createUser(request);
    }

    @PutMapping("/{id}")
    @ApiResponse(message = "User updated successfully")
    @SecuredEndpoint("USER_MANAGE")
    @Operation(summary = "Update user", description = "Update user profile, role, status, verify and lock flags")
    public Object updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestBody @Valid AdminUpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(message = "User deleted successfully")
    @SecuredEndpoint("USER_MANAGE")
    @Operation(summary = "Delete user", description = "Delete a user account by id")
    public Object deleteUser(@Parameter(description = "User ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return null;
    }

    @ApiResponse(message = "Users retrieved successfully")
    @GetMapping("/me")
    @AuthenticatedEndpoint
    @Operation(summary = "Get current user profile", description = "Retrieve profile of the authenticated user")
    public Object getCurrentUserProfile() {
        User currentUser = accountUtils.getCurrentUserWithStore();
        return  UserResponse.toUserResponse(currentUser);
    }
}
