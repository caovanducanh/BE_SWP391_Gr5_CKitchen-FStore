package com.example.demologin.initializer.components;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demologin.entity.Permission;
import com.example.demologin.entity.Role;
import com.example.demologin.repository.PermissionRepository;
import com.example.demologin.repository.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Permission and Role Initializer
 *
 * Responsible for creating all system permissions and roles.
 * This must run before user initialization since users depend on roles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionRoleInitializer {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    // ===================== PERMISSION CODES =====================
    private static final String USER_MANAGE = "USER_MANAGE";  // mới thêm
    private static final String ADMIN_USER_MANAGEMENT = "ADMIN_USER_MANAGEMENT";
    private static final String USER_TOKEN_MANAGEMENT = "USER_TOKEN_MANAGEMENT";
    private static final String TOKEN_INVALIDATE_OWN = "TOKEN_INVALIDATE_OWN";
    private static final String TOKEN_INVALIDATE_USER = "TOKEN_INVALIDATE_USER";
    private static final String TOKEN_VIEW_OWN = "TOKEN_VIEW_OWN";
    private static final String TOKEN_VIEW_USER = "TOKEN_VIEW_USER";

    private static final String ROLE_VIEW = "ROLE_VIEW";
    private static final String ROLE_CREATE = "ROLE_CREATE";
    private static final String ROLE_UPDATE = "ROLE_UPDATE";
    private static final String ROLE_DELETE = "ROLE_DELETE";
    private static final String ROLE_UPDATE_PERMISSIONS = "ROLE_UPDATE_PERMISSIONS";

    private static final String PERMISSION_VIEW = "PERMISSION_VIEW";
    private static final String PERMISSION_UPDATE = "PERMISSION_UPDATE";

    private static final String LOG_VIEW_ACTIVITY = "LOG_VIEW_ACTIVITY";
    private static final String ADMIN_ACTIVITY_LOG_EXPORT = "ADMIN_ACTIVITY_LOG_EXPORT";
    private static final String LOG_DELETE = "LOG_DELETE";

    private static final String USER_VIEW_OWN_LOGIN_HISTORY = "USER_VIEW_OWN_LOGIN_HISTORY";
    private static final String PRODUCT_MANAGE = "PRODUCT_MANAGE";
    private static final String RECIPE_MANAGE = "RECIPE_MANAGE";
    private static final String INVENTORY_VIEW = "INVENTORY_VIEW";
    private static final String INVENTORY_MANAGE = "INVENTORY_MANAGE";
    private static final String MANAGER_DASHBOARD_VIEW = "MANAGER_DASHBOARD_VIEW";
    private static final String STORE_VIEW = "STORE_VIEW";

    // Franchise Store Staff permissions
    private static final String ORDER_VIEW = "ORDER_VIEW";
    private static final String ORDER_CREATE = "ORDER_CREATE";
    private static final String DELIVERY_VIEW = "DELIVERY_VIEW";
    private static final String DELIVERY_CONFIRM = "DELIVERY_CONFIRM";
    private static final String STORE_INVENTORY_VIEW = "STORE_INVENTORY_VIEW";
    private static final String SALES_REPORT_TEMPLATE_DOWNLOAD = "SALES_REPORT_TEMPLATE_DOWNLOAD";
    private static final String SALES_REPORT_IMPORT = "SALES_REPORT_IMPORT";
    private static final String SALES_REPORT_CLEAR = "SALES_REPORT_CLEAR";
    private static final String SALES_REPORT_VIEW_OWN = "SALES_REPORT_VIEW_OWN";
    private static final String SALES_REPORT_VIEW = "SALES_REPORT_VIEW";

    // Central Kitchen Staff permissions
    private static final String ORDER_ASSIGN = "ORDER_ASSIGN";
    private static final String ORDER_STATUS_UPDATE = "ORDER_STATUS_UPDATE";
    private static final String PRODUCTION_PLAN_VIEW = "PRODUCTION_PLAN_VIEW";
    private static final String PRODUCTION_PLAN_CREATE = "PRODUCTION_PLAN_CREATE";
    private static final String PRODUCTION_PLAN_UPDATE = "PRODUCTION_PLAN_UPDATE";
    private static final String KITCHEN_INVENTORY_VIEW = "KITCHEN_INVENTORY_VIEW";
    private static final String KITCHEN_INVENTORY_CREATE = "KITCHEN_INVENTORY_CREATE";  // Thêm mới
    private static final String SYSTEM_CONFIG_MANAGE = "SYSTEM_CONFIG_MANAGE";
    private static final String FRANCHISE_STORE_MANAGE = "FRANCHISE_STORE_MANAGE";
    private static final String CENTRAL_KITCHEN_MANAGE = "CENTRAL_KITCHEN_MANAGE";
    private static final String SYSTEM_REPORT_VIEW = "SYSTEM_REPORT_VIEW";

    // Supply Coordinator permissions
    private static final String SUPPLY_ORDER_VIEW = "SUPPLY_ORDER_VIEW";
    private static final String SUPPLY_ORDER_ASSIGN = "SUPPLY_ORDER_ASSIGN";
    private static final String SUPPLY_DELIVERY_SCHEDULE = "SUPPLY_DELIVERY_SCHEDULE";
    private static final String SUPPLY_DELIVERY_VIEW = "SUPPLY_DELIVERY_VIEW";
    private static final String SUPPLY_DELIVERY_UPDATE = "SUPPLY_DELIVERY_UPDATE";
    private static final String SUPPLY_ISSUE_MANAGE = "SUPPLY_ISSUE_MANAGE";

    // Shipper permissions
    private static final String SHIPPER_DELIVERY_VIEW = "SHIPPER_DELIVERY_VIEW";
    private static final String SHIPPER_DELIVERY_CLAIM = "SHIPPER_DELIVERY_CLAIM";
    private static final String SHIPPER_DELIVERY_UPDATE = "SHIPPER_DELIVERY_UPDATE";

    // ===================== ROLE NAMES =====================
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_SUPPLY_COORDINATOR = "SUPPLY_COORDINATOR";
    private static final String ROLE_CENTRAL_KITCHEN_STAFF = "CENTRAL_KITCHEN_STAFF";
    private static final String ROLE_FRANCHISE_STORE_STAFF = "FRANCHISE_STORE_STAFF";
    private static final String ROLE_SHIPPER = "SHIPPER";

    @Transactional
    public void initializePermissionsAndRoles() {
        log.info("🔑 Initializing system permissions and roles...");

        createPermissions();
        createRoles();

        log.info("✅ Successfully initialized {} permissions and {} roles",
                permissionRepository.count(), roleRepository.count());
    }

    private void createPermissions() {
        log.debug("📋 Creating system permissions...");

        ensurePermission(USER_MANAGE, "Quản lý user (Admin)");
        ensurePermission(ADMIN_USER_MANAGEMENT, "Quản lý phiên đăng nhập của người dùng (Admin)");
        ensurePermission(USER_TOKEN_MANAGEMENT, "Quản lý token của user");
        ensurePermission(TOKEN_INVALIDATE_OWN, "Hủy token của bản thân");
        ensurePermission(TOKEN_INVALIDATE_USER, "Hủy token của user cụ thể");
        ensurePermission(TOKEN_VIEW_OWN, "Xem token version của bản thân");
        ensurePermission(TOKEN_VIEW_USER, "Xem token version của user cụ thể");
        ensurePermission(ROLE_VIEW, "Xem vai trò");
        ensurePermission(ROLE_CREATE, "Tạo vai trò");
        ensurePermission(ROLE_UPDATE, "Cập nhật vai trò");
        ensurePermission(ROLE_DELETE, "Xóa vai trò");
        ensurePermission(ROLE_UPDATE_PERMISSIONS, "Gán quyền cho vai trò");
        ensurePermission(PERMISSION_VIEW, "Xem quyền");
        ensurePermission(PERMISSION_UPDATE, "Cập nhật quyền");
        ensurePermission(LOG_VIEW_ACTIVITY, "Xem user activity logs");
        ensurePermission(ADMIN_ACTIVITY_LOG_EXPORT, "Export user activity logs");
        ensurePermission(LOG_DELETE, "Xóa user activity logs");
        ensurePermission(USER_VIEW_OWN_LOGIN_HISTORY, "Xem lịch sử đăng nhập của bản thân");
        ensurePermission(PRODUCT_MANAGE, "Quản lý sản phẩm");
        ensurePermission(RECIPE_MANAGE, "Quản lý công thức và định mức nguyên liệu");
        ensurePermission(INVENTORY_VIEW, "Xem tồn kho bếp trung tâm và cửa hàng");
        ensurePermission(INVENTORY_MANAGE, "Quản lý tồn kho bếp trung tâm");
        ensurePermission(MANAGER_DASHBOARD_VIEW, "Xem dashboard vận hành manager");
        ensurePermission(STORE_VIEW, "Xem thông tin chi nhánh cửa hàng");
        ensurePermission(ORDER_VIEW, "Xem đơn đặt hàng");
        ensurePermission(ORDER_CREATE, "Tạo đơn đặt hàng nguyên liệu từ bếp trung tâm");
        ensurePermission(DELIVERY_VIEW, "Xem trạng thái giao hàng");
        ensurePermission(DELIVERY_CONFIRM, "Xác nhận nhận hàng và phản hồi chất lượng");
        ensurePermission(STORE_INVENTORY_VIEW, "Xem tồn kho cửa hàng franchise");
        ensurePermission(SALES_REPORT_TEMPLATE_DOWNLOAD, "Tải file excel mẫu để báo cáo doanh số bán hàng");
        ensurePermission(SALES_REPORT_IMPORT, "Import file excel doanh số bán hàng theo ngày");
        ensurePermission(SALES_REPORT_CLEAR, "Xóa báo cáo doanh số theo ngày để import lại");
        ensurePermission(SALES_REPORT_VIEW_OWN, "Xem doanh số theo ngày của cửa hàng đang phụ trách");
        ensurePermission(SALES_REPORT_VIEW, "Xem doanh thu theo ngày của cửa hàng");
        ensurePermission(ORDER_ASSIGN, "Tiếp nhận và gán đơn hàng cho bếp xử lý");
        ensurePermission(ORDER_STATUS_UPDATE, "Cập nhật trạng thái đơn hàng vận hành bếp");
        ensurePermission(PRODUCTION_PLAN_VIEW, "Xem kế hoạch sản xuất");
        ensurePermission(PRODUCTION_PLAN_CREATE, "Tạo kế hoạch sản xuất");
        ensurePermission(PRODUCTION_PLAN_UPDATE, "Cập nhật và điều hành kế hoạch sản xuất");
        ensurePermission(KITCHEN_INVENTORY_VIEW, "Xem tồn kho nguyên liệu bếp trung tâm");
        ensurePermission(KITCHEN_INVENTORY_CREATE, "Nhập lô nguyên liệu mới vào bếp trung tâm");  // Thêm mới
        ensurePermission(SYSTEM_CONFIG_MANAGE, "Quản lý cấu hình hệ thống và tham số vận hành");
        ensurePermission(FRANCHISE_STORE_MANAGE, "Quản lý danh mục cửa hàng franchise");
        ensurePermission(CENTRAL_KITCHEN_MANAGE, "Quản lý danh mục bếp trung tâm");
        ensurePermission(SYSTEM_REPORT_VIEW, "Xem báo cáo tổng hợp toàn hệ thống");
        ensurePermission(SUPPLY_ORDER_VIEW, "Xem và tổng hợp đơn hàng toàn hệ thống cho điều phối");
        ensurePermission(SUPPLY_ORDER_ASSIGN, "Điều phối đơn hàng sang bếp xử lý");
        ensurePermission(SUPPLY_DELIVERY_SCHEDULE, "Lập lịch giao hàng cho đơn");
        ensurePermission(SUPPLY_DELIVERY_VIEW, "Theo dõi danh sách giao hàng điều phối");
        ensurePermission(SUPPLY_DELIVERY_UPDATE, "Cập nhật trạng thái giao hàng");
        ensurePermission(SUPPLY_ISSUE_MANAGE, "Xử lý sự cố thiếu hàng, trễ giao hoặc hủy đơn");
        ensurePermission(SHIPPER_DELIVERY_VIEW, "Xem danh sách đơn chờ nhận và đơn đang giao của shipper");
        ensurePermission(SHIPPER_DELIVERY_CLAIM, "Quét QR để nhận đơn giao");
        ensurePermission(SHIPPER_DELIVERY_UPDATE, "Shipper cập nhật đã giao thành công và chờ cửa hàng xác nhận");

        log.debug("✅ Created {} permissions", permissionRepository.count());
    }

    private void ensurePermission(String code, String description) {
        if (permissionRepository.findByCode(code).isPresent()) {
            return;
        }
        permissionRepository.save(new Permission(code, description));
    }

    private void createRoles() {
        log.debug("👑 Creating system roles...");

        // Tạo map {code -> Permission}
        Map<String, Permission> permMap = permissionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Permission::getCode, p -> p));

        // Admin: full quyền
        Set<Permission> adminPerms = new HashSet<>(permMap.values());

        // Operational roles: quyền giới hạn cơ bản
        Set<Permission> operationalPerms = Set.of(
                permMap.get(USER_TOKEN_MANAGEMENT),
                permMap.get(TOKEN_INVALIDATE_OWN),
                permMap.get(TOKEN_VIEW_OWN),
                permMap.get(USER_VIEW_OWN_LOGIN_HISTORY)
        );

        // Manager: có thêm quyền giám sát
        Set<Permission> managerPerms = new HashSet<>(operationalPerms);
        managerPerms.add(permMap.get(LOG_VIEW_ACTIVITY));
        managerPerms.add(permMap.get(PERMISSION_VIEW));
        managerPerms.add(permMap.get(PRODUCT_MANAGE));
        managerPerms.add(permMap.get(RECIPE_MANAGE));
        managerPerms.add(permMap.get(INVENTORY_VIEW));
        managerPerms.add(permMap.get(INVENTORY_MANAGE));
        managerPerms.add(permMap.get(MANAGER_DASHBOARD_VIEW));
        managerPerms.add(permMap.get(STORE_VIEW));
        managerPerms.add(permMap.get(SALES_REPORT_VIEW));

        // Supply coordinator: nghiệp vụ điều phối + xem log
        Set<Permission> supplyCoordinatorPerms = new HashSet<>(operationalPerms);
        supplyCoordinatorPerms.add(permMap.get(LOG_VIEW_ACTIVITY));
        supplyCoordinatorPerms.add(permMap.get(SUPPLY_ORDER_VIEW));
        supplyCoordinatorPerms.add(permMap.get(SUPPLY_ORDER_ASSIGN));
        supplyCoordinatorPerms.add(permMap.get(SUPPLY_DELIVERY_SCHEDULE));
        supplyCoordinatorPerms.add(permMap.get(SUPPLY_DELIVERY_VIEW));
        supplyCoordinatorPerms.add(permMap.get(SUPPLY_DELIVERY_UPDATE));
        supplyCoordinatorPerms.add(permMap.get(SUPPLY_ISSUE_MANAGE));

        ensureRole(ROLE_ADMIN, adminPerms);
        ensureRole(ROLE_MANAGER, managerPerms);
        ensureRole(ROLE_SUPPLY_COORDINATOR, supplyCoordinatorPerms);

        Set<Permission> centralKitchenPerms = new HashSet<>(operationalPerms);
        centralKitchenPerms.add(permMap.get(ORDER_VIEW));
        centralKitchenPerms.add(permMap.get(ORDER_ASSIGN));
        centralKitchenPerms.add(permMap.get(ORDER_STATUS_UPDATE));
        centralKitchenPerms.add(permMap.get(PRODUCTION_PLAN_VIEW));
        centralKitchenPerms.add(permMap.get(PRODUCTION_PLAN_CREATE));
        centralKitchenPerms.add(permMap.get(PRODUCTION_PLAN_UPDATE));
        centralKitchenPerms.add(permMap.get(KITCHEN_INVENTORY_VIEW));
        centralKitchenPerms.add(permMap.get(KITCHEN_INVENTORY_CREATE));   // Thêm dòng này
        centralKitchenPerms.add(permMap.get(STORE_VIEW));
        ensureRole(ROLE_CENTRAL_KITCHEN_STAFF, centralKitchenPerms);

        // Franchise Store Staff: thêm quyền nghiệp vụ đặt hàng và tồn kho
        Set<Permission> franchiseStorePerms = new HashSet<>(operationalPerms);
        franchiseStorePerms.add(permMap.get(ORDER_VIEW));
        franchiseStorePerms.add(permMap.get(ORDER_CREATE));
        franchiseStorePerms.add(permMap.get(DELIVERY_VIEW));
        franchiseStorePerms.add(permMap.get(DELIVERY_CONFIRM));
        franchiseStorePerms.add(permMap.get(STORE_INVENTORY_VIEW));
        franchiseStorePerms.add(permMap.get(STORE_VIEW));
        franchiseStorePerms.add(permMap.get(SALES_REPORT_TEMPLATE_DOWNLOAD));
        franchiseStorePerms.add(permMap.get(SALES_REPORT_IMPORT));
        franchiseStorePerms.add(permMap.get(SALES_REPORT_CLEAR));
        franchiseStorePerms.add(permMap.get(SALES_REPORT_VIEW_OWN));
        ensureRole(ROLE_FRANCHISE_STORE_STAFF, franchiseStorePerms);

        Set<Permission> shipperPerms = new HashSet<>(operationalPerms);
        shipperPerms.add(permMap.get(SHIPPER_DELIVERY_VIEW));
        shipperPerms.add(permMap.get(SHIPPER_DELIVERY_CLAIM));
        shipperPerms.add(permMap.get(SHIPPER_DELIVERY_UPDATE));
        ensureRole(ROLE_SHIPPER, shipperPerms);

        log.debug("✅ Created {} roles", roleRepository.count());
    }

    private void ensureRole(String roleName, Set<Permission> permissions) {
        Set<Permission> validPermissions = permissions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var existingRole = roleRepository.findByName(roleName);
        if (existingRole.isPresent()) {
            Role role = existingRole.get();
            Set<Permission> currentPermissions = role.getPermissions() == null
                    ? new HashSet<>()
                    : new HashSet<>(role.getPermissions());

            boolean changed = currentPermissions.addAll(validPermissions);
            if (changed) {
                role.setPermissions(currentPermissions);
                roleRepository.save(role);
            }
            return;
        }

        roleRepository.save(Role.builder()
                .name(roleName)
                .permissions(validPermissions)
                .build());
    }
}