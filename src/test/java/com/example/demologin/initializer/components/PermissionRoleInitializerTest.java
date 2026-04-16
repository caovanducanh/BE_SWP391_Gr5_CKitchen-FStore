package com.example.demologin.initializer.components;

import com.example.demologin.entity.Permission;
import com.example.demologin.entity.Role;
import com.example.demologin.repository.PermissionRepository;
import com.example.demologin.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRoleInitializerTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleRepository roleRepository;

    private PermissionRoleInitializer initializer;

    private Map<String, Permission> permissionStore;
    private Map<String, Role> roleStore;

    @BeforeEach
    void setUp() {
        initializer = new PermissionRoleInitializer(permissionRepository, roleRepository);
        permissionStore = new HashMap<>();
        roleStore = new HashMap<>();

        when(permissionRepository.findByCode(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(permissionStore.get(inv.getArgument(0))));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> {
            Permission permission = inv.getArgument(0);
            permissionStore.put(permission.getCode(), permission);
            return permission;
        });
        when(permissionRepository.findAll()).thenAnswer(inv -> new ArrayList<>(permissionStore.values()));
        when(permissionRepository.count()).thenAnswer(inv -> (long) permissionStore.size());

        when(roleRepository.findByName(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(roleStore.get(inv.getArgument(0))));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> {
            Role role = inv.getArgument(0);
            roleStore.put(role.getName(), role);
            return role;
        });
        when(roleRepository.count()).thenAnswer(inv -> (long) roleStore.size());
    }

    @Test
    void initializePermissionsAndRoles_shouldCreateManagerRoleWithManagerPermissions() {
        initializer.initializePermissionsAndRoles();

        Role managerRole = roleStore.get("MANAGER");
        Set<String> managerPermissionCodes = managerRole.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        assertTrue(managerPermissionCodes.contains("PRODUCT_MANAGE"));
        assertTrue(managerPermissionCodes.contains("RECIPE_MANAGE"));
        assertTrue(managerPermissionCodes.contains("INVENTORY_VIEW"));
        assertTrue(managerPermissionCodes.contains("MANAGER_DASHBOARD_VIEW"));
        assertTrue(managerPermissionCodes.contains("LOG_VIEW_ACTIVITY"));
    }

    @Test
    void initializePermissionsAndRoles_shouldMergePermissionsForExistingManagerRole() {
        Role existingManager = Role.builder().name("MANAGER").permissions(new HashSet<>()).build();
        roleStore.put("MANAGER", existingManager);

        initializer.initializePermissionsAndRoles();

        Role managerRole = roleStore.get("MANAGER");
        Set<String> managerPermissionCodes = managerRole.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        assertTrue(managerPermissionCodes.contains("PRODUCT_MANAGE"));
        assertTrue(managerPermissionCodes.contains("RECIPE_MANAGE"));
        assertTrue(managerPermissionCodes.contains("MANAGER_DASHBOARD_VIEW"));
    }
}
