package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.admin.AdminCreateUserRequest;
import com.example.demologin.dto.request.admin.AdminUpdateUserRequest;
import com.example.demologin.dto.response.MemberResponse;
import com.example.demologin.entity.Role;
import com.example.demologin.entity.User;
import com.example.demologin.enums.UserStatus;
import com.example.demologin.exception.exceptions.ConflictException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.mapper.UserMapper;
import com.example.demologin.repository.RoleRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

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
    public MemberResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        String normalizedRoleName = request.getRoleName().trim();
        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new NotFoundException("Role " + normalizedRoleName + " not found"));

        User user = new User(
                request.getUsername().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getEmail().trim()
        );
        user.setRole(role);
        user.setStatus(request.getStatus());
        user.setVerify(request.getVerify() != null && request.getVerify());
        user.setLocked(false);

        User savedUser = userRepository.save(user);
        return userMapper.toUserResponse(savedUser);
    }

    @Override
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
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));
        userRepository.delete(user);
    }
}
