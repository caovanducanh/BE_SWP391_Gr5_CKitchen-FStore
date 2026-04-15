package com.example.demologin.initializer.components;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.entity.Role;
import com.example.demologin.entity.User;
import com.example.demologin.enums.UserStatus;
import com.example.demologin.repository.RoleRepository;
import com.example.demologin.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default User Initializer
 * 
 * Responsible for creating default system users with appropriate roles.
 * This runs after PermissionRoleInitializer since users depend on roles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultUserInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void initializeDefaultUsers() {
        log.info("👥 Initializing default system users...");

        createDefaultUsers();
        
        log.info("✅ Successfully initialized {} default users", userRepository.count());
    }

    private void createDefaultUsers() {
        log.debug("👤 Creating default system users...");
        
        createUser("admin", "admin123", "ADMIN");
        createUser("manager", "manager123", "MANAGER");
        createUser("supply", "supply123", "SUPPLY_COORDINATOR");
        createUser("kitchen", "kitchen123", "CENTRAL_KITCHEN_STAFF");
        createUser("storestaff", "store123", "FRANCHISE_STORE_STAFF");
        createUser("shipper", "shipper123", "SHIPPER");
        
        log.debug("✅ Created {} users", userRepository.count());
    }

    private void createUser(String username, String rawPassword, String roleName) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.debug("⚠️ User '{}' already exists, skipping", username);
            return;
        }
        
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Role '" + roleName + "' not found. PermissionRoleInitializer must run first."));
        
        User user = new User(
                username,
                passwordEncoder.encode(rawPassword),
                username + " Fullname",
            username + "@example.com"
        );
        
        // Set additional properties
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setTokenVersion(0);
        user.setVerify(true);
        user.setLocked(false);
        
        userRepository.save(user);
        log.debug("✅ Created user '{}' with role '{}'", username, roleName);
    }
}
