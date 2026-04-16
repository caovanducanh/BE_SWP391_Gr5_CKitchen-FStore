package com.example.demologin.initializer.components;

import com.example.demologin.entity.Role;
import com.example.demologin.entity.User;
import com.example.demologin.enums.UserStatus;
import com.example.demologin.repository.RoleRepository;
import com.example.demologin.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUserInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DefaultUserInitializer initializer;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "enc-" + inv.getArgument(0));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.count()).thenReturn(6L);
    }

    @Test
    void initializeDefaultUsers_shouldCreateManagerUserWithManagerRole() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName(anyString())).thenAnswer(inv -> {
            String roleName = inv.getArgument(0);
            return Optional.of(Role.builder().name(roleName).build());
        });

        initializer.initializeDefaultUsers();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(6)).save(captor.capture());

        List<User> savedUsers = captor.getAllValues();
        User manager = savedUsers.stream().filter(u -> "manager".equals(u.getUsername())).findFirst().orElseThrow();

        assertEquals("MANAGER", manager.getRole().getName());
        assertEquals(UserStatus.ACTIVE, manager.getStatus());
        assertTrue(manager.isVerify());
        assertFalse(manager.isLocked());
        assertEquals("enc-manager123", manager.getPassword());
    }

    @Test
    void initializeDefaultUsers_shouldThrowWhenManagerRoleMissing() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(Role.builder().name("ADMIN").build()));
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> initializer.initializeDefaultUsers());
    }
}
