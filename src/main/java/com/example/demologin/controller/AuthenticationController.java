package com.example.demologin.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.annotation.UserActivity;
import com.example.demologin.dto.request.login.FacebookLoginRequest;
import com.example.demologin.dto.request.login.GoogleLoginRequest;
import com.example.demologin.dto.request.login.LoginRequest;
import com.example.demologin.dto.request.token.TokenRefreshRequest;
import com.example.demologin.dto.request.user.UserRegistrationRequest;
import com.example.demologin.enums.ActivityType;
import com.example.demologin.service.AuthenticationService;
import com.example.demologin.service.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("api")
@Tag(name = "Authentication", description = "APIs for user authentication, registration, and OAuth login")
    public class AuthenticationController {

        private static final String TEST_ACCOUNTS_DOC = "Available test accounts: "
                + "ADMIN(admin/admin123), "
                + "MANAGER(manager/manager123), "
                + "SUPPLY_COORDINATOR(supply/supply123), "
                + "CENTRAL_KITCHEN_STAFF(kitchen/kitchen123), "
                + "FRANCHISE_STORE_STAFF(storestaff/store123), "
                + "SHIPPER(shipper/shipper123).";


    private final AuthenticationService authenticationService;


    private final RefreshTokenService refreshTokenService;

    @PublicEndpoint
    @PostMapping("/register")
    @ApiResponse(message = "User registered successfully")
    @UserActivity(activityType = ActivityType.REGISTRATION, details = "User registration")
    @Operation(summary = "User registration", 
               description = "Register a new user account with email verification")
    public Object register(@Valid @RequestBody UserRegistrationRequest request) {
        return authenticationService.register(request);
    }

    @PublicEndpoint
    @PostMapping("/login")
    @ApiResponse(message = "Login successful")
    @UserActivity(activityType = ActivityType.LOGIN_ATTEMPT, details = "User login attempt")
    @Operation(summary = "User login", 
               description = "Authenticate user with email and password. " + TEST_ACCOUNTS_DOC)
    public Object login(@RequestBody @Valid LoginRequest loginRequest) {
        return authenticationService.login(loginRequest);
    }

    @SecuredEndpoint("USER_TOKEN_MANAGEMENT")
    @PostMapping("/refresh-token")
    @PublicEndpoint
    @ApiResponse(message = "Token refreshed successfully")
    @UserActivity(activityType = ActivityType.TOKEN_REFRESH, details = "Token refresh request")
    @Operation(summary = "Refresh access token", 
               description = "Get new access token using refresh token")
    public Object refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        return refreshTokenService.refreshToken(request.getRefreshToken());
    }

    @PublicEndpoint
    @PostMapping("/google-login")
    @ApiResponse(message = "Google login successful")
    @UserActivity(activityType = ActivityType.LOGIN_ATTEMPT, details = "Google OAuth login attempt")
    @Operation(summary = "Google OAuth login", 
               description = "Authenticate user with Google OAuth token. Only pre-created accounts in the system are allowed; auto-registration is disabled.")
    public Object loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        return authenticationService.authenticateWithGoogle(request);
    }

    @SecuredEndpoint("USER_TOKEN_MANAGEMENT")
    @GetMapping("/oauth2/success")
    @ApiResponse(message = "OAuth2 login successful")
    @UserActivity(activityType = ActivityType.LOGIN_SUCCESS, details = "OAuth2 login successful")
    @Operation(summary = "OAuth2 login success callback", 
               description = "Handle successful OAuth2 authentication callback")
    public Object oauth2LoginSuccess(Authentication authentication) {
        return authenticationService.authenticateWithOAuth2FromAuthentication(authentication);
    }

    @PublicEndpoint
    @PostMapping("/facebook-login")
    @ApiResponse(message = "Facebook login successful")
    @UserActivity(activityType = ActivityType.LOGIN_ATTEMPT, details = "Facebook OAuth login attempt")
    @Operation(summary = "Facebook OAuth login", 
               description = "Authenticate user with Facebook OAuth token")
    public Object loginWithFacebook(@RequestBody FacebookLoginRequest request) {
        return authenticationService.authenticateWithFacebook(request);
    }
}
