package com.example.demologin.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.annotation.UserActivity;
import com.example.demologin.dto.request.emailOTP.ActivateAccountRequest;
import com.example.demologin.dto.request.emailOTP.EmailRequest;
import com.example.demologin.dto.request.emailOTP.OtpRequest;
import com.example.demologin.dto.request.emailOTP.ResetPasswordRequestWithOtp;
import com.example.demologin.enums.ActivityType;
import com.example.demologin.service.EmailOtpService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Tag(name = "Email OTP", description = "APIs for email verification and password reset using OTP")
public class EmailOtpController {
    
    private final EmailOtpService emailOtpService;

    @PublicEndpoint
    @PostMapping("/send-verification")
    @ApiResponse(message = "Verification OTP sent successfully")
    @UserActivity(activityType = ActivityType.EMAIL_VERIFICATION, details = "Email verification OTP sent")
    @Operation(summary = "Send email verification OTP", 
               description = "Send OTP to email for email verification during registration")
    public Object sendVerificationOtp(@RequestBody @Valid EmailRequest request) {
        return emailOtpService.sendVerificationOtp(request);
    }

    @PublicEndpoint
    @PostMapping("/verify")
    @ApiResponse(message = "Email verified successfully")
    @UserActivity(activityType = ActivityType.OTP_VERIFICATION, details = "Email OTP verification attempt")
    @Operation(summary = "Verify email OTP", 
               description = "Verify the OTP code sent to email")
    public Object verifyEmailOtp(@RequestBody @Valid OtpRequest request) {
        return emailOtpService.verifyEmailOtp(request);
    }

    @PublicEndpoint
    @PostMapping("/forgot-password")
    @ApiResponse(message = "Password reset OTP sent successfully")
    @UserActivity(activityType = ActivityType.EMAIL_VERIFICATION, details = "Forgot password OTP sent")
    @Operation(summary = "Send forgot password OTP", 
               description = "Send OTP to email for password reset")
    public Object sendForgotPasswordOtp(@RequestBody @Valid EmailRequest request) {
        return emailOtpService.sendForgotPasswordOtp(request);
    }

    @PublicEndpoint
    @PostMapping("/verify-forgot-password")
    @ApiResponse(message = "Forgot password OTP verified successfully")
    @UserActivity(activityType = ActivityType.OTP_VERIFICATION, details = "Verify forgot password OTP")
    @Operation(summary = "Verify forgot password OTP", 
               description = "Verify the OTP code sent to email for password reset")
    public Object verifyForgotPasswordOtp(@RequestBody @Valid OtpRequest request) {
        return emailOtpService.verifyForgotPasswordOtp(request);
    }

    @PublicEndpoint
    @PostMapping("/reset-password")
    @ApiResponse(message = "Password reset successfully")
    @UserActivity(activityType = ActivityType.PASSWORD_CHANGE, details = "Password reset with OTP")
    @Operation(summary = "Reset password with OTP", 
               description = "Reset user password using verified OTP")
    public Object resetPasswordWithOtp(@RequestBody @Valid ResetPasswordRequestWithOtp request) {
        return emailOtpService.resetPasswordWithOtp(request);
    }

    @PublicEndpoint
    @PostMapping("/resend")
    @ApiResponse(message = "OTP resent successfully")
    @UserActivity(activityType = ActivityType.EMAIL_VERIFICATION, details = "OTP resend request")
    @Operation(summary = "Resend OTP", 
               description = "Resend OTP to the same email address")
    public Object resendOtp(@RequestBody @Valid EmailRequest request) {
        return emailOtpService.resendOtp(request);
    }

    @PublicEndpoint
    @PostMapping("/activate-account")
    @ApiResponse(message = "Account activated successfully")
    @UserActivity(activityType = ActivityType.EMAIL_VERIFICATION, details = "Account activation with invite OTP")
    @Operation(summary = "Activate invited account",
               description = "Activate account using invite OTP and set username/password/full name")
    public Object activateAccount(@RequestBody @Valid ActivateAccountRequest request) {
        return emailOtpService.activateAccount(request);
    }
}
