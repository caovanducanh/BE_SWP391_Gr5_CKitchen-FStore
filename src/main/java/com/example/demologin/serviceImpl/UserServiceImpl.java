package com.example.demologin.serviceImpl;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.dto.request.admin.AdminCreateUserRequest;
import com.example.demologin.dto.request.admin.AdminUpdateUserRequest;
import com.example.demologin.dto.response.MemberResponse;
import com.example.demologin.entity.EmailOtp;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.Role;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.User;
import com.example.demologin.enums.UserStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.ConflictException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.UserMapper;
import com.example.demologin.repository.EmailOtpRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.RoleRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.repository.RefreshTokenRepository;
import com.example.demologin.service.EmailService;
import com.example.demologin.service.UserService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String ROLE_STORE_STAFF = "FRANCHISE_STORE_STAFF";
    private static final String ROLE_KITCHEN_STAFF = "CENTRAL_KITCHEN_STAFF";
    private static final String TYPE_INVITE = "INVITE_ACCOUNT";
    private static final int INVITE_EXPIRE_MINUTES = 10080; // 7 days

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final KitchenRepository kitchenRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final EmailService emailService;
    private final Environment environment;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Page<MemberResponse> getAllUsers(String roleName, UserStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<User> users;

        if (roleName != null && !roleName.isBlank() && status != null) {
            users = userRepository.findByRole_NameAndStatus(roleName.trim(), status, pageable);
        } else if (roleName != null && !roleName.isBlank()) {
            users = userRepository.findByRole_Name(roleName.trim(), pageable);
        } else if (status != null) {
            users = userRepository.findByStatus(status, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(userMapper::toUserResponse);
    }

    @Override
    public MemberResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public MemberResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        String normalizedRoleName = request.getRoleName().trim();
        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new NotFoundException("Role " + normalizedRoleName + " not found"));

        Store store = null;
        Kitchen kitchen = null;
        if (ROLE_STORE_STAFF.equalsIgnoreCase(normalizedRoleName)) {
            if (request.getStoreId() == null || request.getStoreId().isBlank()) {
                throw new BadRequestException("Store is required for store staff role");
            }
            store = storeRepository.findById(request.getStoreId().trim())
                    .orElseThrow(() -> new NotFoundException("Store not found: " + request.getStoreId()));
        }
        if (ROLE_KITCHEN_STAFF.equalsIgnoreCase(normalizedRoleName)) {
            if (request.getKitchenId() == null || request.getKitchenId().isBlank()) {
                throw new BadRequestException("Kitchen is required for kitchen staff role");
            }
            kitchen = kitchenRepository.findById(request.getKitchenId().trim())
                    .orElseThrow(() -> new NotFoundException("Kitchen not found: " + request.getKitchenId()));
        }

        User user = new User(
                generatePendingUsername(),
                passwordEncoder.encode(generateTempPassword()),
                "Chưa cập nhật",
                request.getEmail().trim()
        );
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setVerify(false);
        user.setLocked(false);
        if (store != null) {
            user.setStore(store);
        }
        if (kitchen != null) {
            user.setKitchen(kitchen);
        }

        User savedUser = userRepository.save(user);
        sendInviteEmail(savedUser);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public MemberResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim();
            if (userRepository.existsByEmailAndUserIdNot(email, userId)) {
                throw new ConflictException("Email already exists");
            }
            user.setEmail(email);
        }

        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            String roleName = request.getRoleName().trim();
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new NotFoundException("Role " + roleName + " not found"));
            user.setRole(role);
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (request.getVerify() != null) {
            user.setVerify(request.getVerify());
        }

        if (request.getLocked() != null) {
            user.setLocked(request.getLocked());
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));
        
        // Delete related refresh tokens to prevent foreign key constraint violation
        refreshTokenRepository.deleteByUser(user);
        
        userRepository.delete(user);
    }

    private String generatePendingUsername() {
        String candidate;
        do {
            candidate = "pending_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (userRepository.existsByUsername(candidate));
        return candidate;
    }

    private String generateTempPassword() {
        int suffix = new Random().nextInt(900000) + 100000;
        return "Temp@" + UUID.randomUUID().toString().substring(0, 6) + suffix;
    }

    private void sendInviteEmail(User user) {
        emailOtpRepository.deleteByEmailAndType(user.getEmail(), TYPE_INVITE);
        String otp = generateOtp();

        EmailOtp entity = EmailOtp.builder()
                .email(user.getEmail())
                .otp(otp)
                .type(TYPE_INVITE)
                .expiredAt(LocalDateTime.now().plusMinutes(INVITE_EXPIRE_MINUTES))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();
        emailOtpRepository.save(entity);

        String baseUrl = environment.getProperty("frontend.base-url");
        String activationUrl = normalizeBaseUrl(baseUrl)
                + "activate-account?email=" + user.getEmail()
                + "&otp=" + otp;

        String subject = "Kích hoạt tài khoản CKitchen";
        
        String htmlBody = "<div style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">"
            + "<h2 style=\"color: #fca5a5; text-align: center; border-bottom: 2px solid #fca5a5; padding-bottom: 10px;\">Chào mừng đến với CKitchen!</h2>"
            + "<p style=\"font-size: 16px;\">Xin chào,</p>"
            + "<p style=\"font-size: 16px;\">Tài khoản của bạn đã được tạo thành công trên hệ thống <strong>CKitchen</strong>.</p>"
            + "<p style=\"font-size: 16px;\">Vui lòng nhấn vào nút bên dưới để xác nhận và hoàn tất thông tin đăng nhập của bạn:</p>"
            + "<div style=\"text-align: center; margin: 30px 0;\">"
            + "  <a href=\"" + activationUrl + "\" style=\"background-color: #f87171; color: white; padding: 12px 24px; text-decoration: none; font-size: 16px; font-weight: bold; border-radius: 6px; display: inline-block;\">Kích Hoạt Tài Khoản</a>"
            + "</div>"
            + "<p style=\"font-size: 14px; color: #555;\">Hoặc bạn có thể sao chép và dán đường dẫn sau vào trình duyệt:</p>"
            + "<p style=\"font-size: 14px; background: #f9f9f9; padding: 10px; border-radius: 4px; word-break: break-all; color: #0056b3;\">" + activationUrl + "</p>"
            + "<p style=\"font-size: 14px; color: #d9534f; margin-top: 20px;\"><em>Lưu ý: Liên kết này có hiệu lực trong vòng 7 ngày.</em></p>"
            + "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\" />"
            + "<p style=\"font-size: 12px; color: #888; text-align: center;\">Đây là email tự động, vui lòng không trả lời email này.</p>"
            + "</div>";

        emailService.sendHtmlEmail(user.getEmail(), subject, htmlBody);
    }

    private String generateOtp() {
        int otp = 100000 + new Random().nextInt(900000);
        return String.valueOf(otp);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("frontend.base-url is not configured in environment");
        }
        String normalized = baseUrl.trim();
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }
}
