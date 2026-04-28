package com.example.demologin.service;

import org.springframework.http.ResponseEntity;

import com.example.demologin.dto.request.emailOTP.ActivateAccountRequest;
import com.example.demologin.dto.request.emailOTP.EmailRequest;
import com.example.demologin.dto.request.emailOTP.OtpRequest;
import com.example.demologin.dto.request.emailOTP.ResetPasswordRequestWithOtp;
import com.example.demologin.dto.response.ResponseObject;

public interface EmailOtpService {
    ResponseEntity<ResponseObject> sendVerificationOtp(EmailRequest request);
    ResponseEntity<ResponseObject> verifyEmailOtp(OtpRequest request);
    ResponseEntity<ResponseObject> sendForgotPasswordOtp(EmailRequest request);
    ResponseEntity<ResponseObject> verifyForgotPasswordOtp(OtpRequest request);
    ResponseEntity<ResponseObject> resetPasswordWithOtp(ResetPasswordRequestWithOtp request);
    ResponseEntity<ResponseObject> resendOtp(EmailRequest request);
    ResponseEntity<ResponseObject> activateAccount(ActivateAccountRequest request);
} 
