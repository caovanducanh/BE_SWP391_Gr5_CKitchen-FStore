package com.example.demologin.controller;

import com.example.demologin.service.RoleService;
import com.example.demologin.dto.request.role.UpdateRoleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.example.demologin.dto.response.RoleResponse;
import java.util.Collections;
import java.util.List;

class RoleControllerTest {
    @Test
    void updatePermissions() {
        com.example.demologin.dto.request.role.RolePermissionsRequest req = new com.example.demologin.dto.request.role.RolePermissionsRequest();
        com.example.demologin.dto.response.RoleResponse resp = new com.example.demologin.dto.response.RoleResponse();
        when(roleService.updatePermissions(2L, req)).thenReturn(resp);
        Object result = controller.updatePermissions(2L, req);
        assertEquals(resp, result);
        verify(roleService).updatePermissions(2L, req);
    }

    @Test
    void getById() {
        com.example.demologin.dto.response.RoleResponse resp = new com.example.demologin.dto.response.RoleResponse();
        when(roleService.getById(3L)).thenReturn(resp);
        Object result = controller.getById(3L);
        assertEquals(resp, result);
        verify(roleService).getById(3L);
    }
    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAll() {
        List<RoleResponse> roles = Collections.emptyList();
        when(roleService.getAll()).thenReturn(roles);
        Object result = controller.getAll();
        assertEquals(roles, result);
        verify(roleService).getAll();
    }

    @Test
    void update() {
        UpdateRoleRequest req = new UpdateRoleRequest();
        RoleResponse resp = new RoleResponse();
        when(roleService.update(1L, req)).thenReturn(resp);
        Object result = controller.update(1L, req);
        assertEquals(resp, result);
        verify(roleService).update(1L, req);
    }
}
