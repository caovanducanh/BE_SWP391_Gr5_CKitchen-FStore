package com.example.demologin.serviceImpl;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.dto.request.emailOTP.ActivateAccountRequest;
import com.example.demologin.dto.request.emailOTP.EmailRequest;
import com.example.demologin.dto.request.emailOTP.OtpRequest;
import com.example.demologin.dto.request.emailOTP.ResetPasswordRequestWithOtp;
import com.example.demologin.dto.response.ResponseObject;
import com.example.demologin.entity.EmailOtp;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.ConflictException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.EmailOtpRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.EmailOtpService;
import com.example.demologin.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class EmailOtpServiceImpl implements EmailOtpService {
    
    private static final int OTP_EXPIRE_MINUTES = 5;
    private static final String TYPE_VERIFY = "VERIFY_EMAIL";
    private static final String TYPE_FORGOT = "FORGOT_PASSWORD";
    private static final String TYPE_INVITE = "INVITE_ACCOUNT";

    private final EmailOtpRepository emailOtpRepo;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseObject> sendVerificationOtp(EmailRequest request) {
        String email = request.getEmail();
        // Chỉ gửi OTP xác thực cho email đã đăng ký
        if (!userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email not found");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found with email: " + email));
        if (user.isVerify()) {
            throw new BadRequestException("Email has already been verified");
        }
        emailOtpRepo.deleteByEmailAndType(email, TYPE_VERIFY);
        String otp = generateOtp();
        EmailOtp entity = EmailOtp.builder()
                .email(email)
                .otp(otp)
                .type(TYPE_VERIFY)
                .expiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();
        emailOtpRepo.save(entity);
        emailService.sendEmail(email, "Email Verification OTP", "Your OTP is: " + otp);
        ResponseObject response = new com.example.demologin.dto.response.ResponseObject(200, "OTP sent successfully", null);
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseObject> verifyEmailOtp(OtpRequest request) {
        EmailOtp otpEntity = emailOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(request.getEmail(), TYPE_VERIFY)
                .orElseThrow(() -> new BadRequestException("No OTP found for this email"));
        if (otpEntity.isVerified()) throw new BadRequestException("OTP already used");
        if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) throw new BadRequestException("OTP expired");
        if (!otpEntity.getOtp().equals(request.getOtp())) throw new BadRequestException("OTP invalid");
        otpEntity.setVerified(true);
        emailOtpRepo.save(otpEntity);
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            user.setVerify(true);
            userRepository.save(user);
        });
        // Xóa OTP sau khi dùng xong
        emailOtpRepo.deleteById(otpEntity.getId());
        log.debug("OTP deleted after successful verification for email: {}", request.getEmail());
        ResponseObject response = new com.example.demologin.dto.response.ResponseObject(200, "OTP verified successfully", null);
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseObject> sendForgotPasswordOtp(EmailRequest request) {
        String email = request.getEmail();
        if (!userRepository.existsByEmail(email)) throw new BadRequestException("Email not found");

        emailOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(email, TYPE_FORGOT).ifPresent(latestOtp -> {
            if (latestOtp.getCreatedAt().plusMinutes(1).isAfter(LocalDateTime.now())) {
                throw new BadRequestException("Vui lòng đợi 1 phút trước khi yêu cầu OTP mới.");
            }
        });

        emailOtpRepo.deleteByEmailAndType(email, TYPE_FORGOT);
        String otp = generateOtp();
        EmailOtp entity = EmailOtp.builder()
                .email(email)
                .otp(otp)
                .type(TYPE_FORGOT)
                .expiredAt(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();
        emailOtpRepo.save(entity);

        String subject = "Mã xác thực Đặt lại mật khẩu";
        String htmlBody = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">"
            + "<h2 style=\"color: #fca5a5; text-align: center; border-bottom: 2px solid #fca5a5; padding-bottom: 10px;\">Khôi phục mật khẩu CKitchen</h2>"
            + "<p style=\"font-size: 16px;\">Xin chào,</p>"
            + "<p style=\"font-size: 16px;\">Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng sử dụng mã xác thực (OTP) dưới đây để tiếp tục:</p>"
            + "<div style=\"text-align: center; margin: 30px 0;\">"
            + "  <span style=\"background-color: #f87171; color: white; padding: 15px 30px; font-size: 24px; font-weight: bold; border-radius: 8px; display: inline-block; letter-spacing: 5px;\">" + otp + "</span>"
            + "</div>"
            + "<p style=\"font-size: 14px; color: #d9534f; margin-top: 20px;\"><em>Lưu ý: Mã OTP này có hiệu lực trong vòng " + OTP_EXPIRE_MINUTES + " phút.</em></p>"
            + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\" />"
            + "<p style=\"font-size: 12px; color: #888; text-align: center;\">Nếu bạn không yêu cầu đổi mật khẩu, vui lòng bỏ qua email này.</p>"
            + "</div>";

        emailService.sendHtmlEmail(email, subject, htmlBody);
        
        ResponseObject response = new com.example.demologin.dto.response.ResponseObject(200, "Forgot password OTP sent successfully", null);
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseObject> resetPasswordWithOtp(ResetPasswordRequestWithOtp req) {
        EmailOtp otpEntity = emailOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(req.getEmail(), TYPE_FORGOT)
                .orElseThrow(() -> new BadRequestException("No OTP found for this email"));
        if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) throw new BadRequestException("OTP expired");
        if (!otpEntity.getOtp().equals(req.getOtp())) throw new BadRequestException("OTP invalid");
        
        User user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new BadRequestException("User not found"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        emailOtpRepo.deleteById(otpEntity.getId());
        log.debug("OTP deleted after successful password reset for email: {}", req.getEmail());
        ResponseObject response = new ResponseObject(200, "Password reset successfully", null);
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseObject> verifyForgotPasswordOtp(OtpRequest request) {
        EmailOtp otpEntity = emailOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(request.getEmail(), TYPE_FORGOT)
                .orElseThrow(() -> new BadRequestException("No OTP found for this email"));
        if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) throw new BadRequestException("OTP expired");
        if (!otpEntity.getOtp().equals(request.getOtp())) throw new BadRequestException("OTP invalid");
        
        // Return success but don't delete the OTP yet, it will be deleted when they actually reset the password
        ResponseObject response = new ResponseObject(200, "OTP verified successfully", null);
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseObject> activateAccount(ActivateAccountRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }

        EmailOtp otpEntity = emailOtpRepo.findTopByEmailAndTypeOrderByCreatedAtDesc(request.getEmail(), TYPE_INVITE)
                .orElseThrow(() -> new BadRequestException("No OTP found for this email"));
        if (otpEntity.isVerified()) {
            throw new BadRequestException("OTP already used");
        }
        if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired");
        }
        if (!otpEntity.getOtp().equals(request.getOtp())) {
            throw new BadRequestException("OTP invalid");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found with email: " + request.getEmail()));
        if (user.isVerify()) {
            throw new BadRequestException("Account already activated");
        }

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setVerify(true);
        userRepository.save(user);

        otpEntity.setVerified(true);
        emailOtpRepo.save(otpEntity);
        emailOtpRepo.deleteById(otpEntity.getId());
        log.debug("Invite OTP deleted after activation for email: {}", request.getEmail());
        ResponseObject response = new ResponseObject(200, "Account activated successfully", null);
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseObject> resendOtp(EmailRequest request) {
        // Xác định loại OTP cần resend (ở đây mặc định resend cho VERIFY_EMAIL)
        return sendVerificationOtp(request);
    }

    // Xóa OTP hết hạn định kỳ
    @Scheduled(fixedRate = 5 * 60 * 1000) // mỗi 5 phút
    @Transactional
    public void deleteExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        
        // Thực hiện DELETE trực tiếp và chỉ log khi có OTP bị xóa
        int deletedCount = emailOtpRepo.deleteExpiredOtps(now);
        
        if (deletedCount > 0) {
            log.info("Deleted {} expired OTP records", deletedCount);
        }
        // Khi deletedCount = 0, không log gì và chỉ có 1 DELETE query duy nhất
    }
} 
