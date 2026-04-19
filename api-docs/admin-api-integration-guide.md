# Admin API Integration Guide

Tài liệu này tổng hợp đầy đủ các API admin hiện tại để FE tích hợp nhanh và đúng payload.

## 1. Base URL và Authentication

- Base URL local: `http://localhost:8080`
- Tất cả API trong tài liệu này đều cần Bearer token admin hợp lệ.

Header mẫu:
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

## 2. Chuẩn response

Phần lớn endpoint có `@ApiResponse` nên backend wrap theo `ResponseObject`:

```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

Với endpoint trả phân trang (`@PageResponse`), `data` sẽ có dạng:

```json
{
  "content": [],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

## 3. Enum dùng trong Admin APIs

- `UserStatus`: `ACTIVE`, `DISABLED`
- Store/Kitchen status: `ACTIVE`, `INACTIVE`
- Priority code: `HIGH`, `NORMAL`, `LOW`

## 4. User Management (`/api/admin/users`)

Permission: `USER_MANAGE`

### 4.1 Lấy danh sách user (phân trang + filter)
- `GET /api/admin/users?roleName=ADMIN&status=ACTIVE&page=0&size=20`

Query params:
- `roleName` (optional): tên role
- `status` (optional): `ACTIVE|DISABLED`
- `page` (optional, default `0`)
- `size` (optional, default `20`)

Response mẫu:
```json
{
  "statusCode": 200,
  "message": "Users retrieved successfully",
  "data": {
    "content": [
      {
        "userId": 1,
        "username": "admin",
        "email": "admin@example.com",
        "identity_Card": null,
        "fullName": "System Admin",
        "phone": null,
        "address": null,
        "dateOfBirth": null,
        "status": "ACTIVE",
        "role": "ADMIN"
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

### 4.2 Lấy chi tiết user
- `GET /api/admin/users/{id}`

Response: `MemberResponse` trong `data`.

### 4.3 Tạo user
- `POST /api/admin/users`

Body:
```json
{
  "username": "staff_01",
  "password": "Aa@123456",
  "fullName": "Store Staff 01",
  "email": "staff01@example.com",
  "roleName": "FRANCHISE_STORE_STAFF",
  "status": "ACTIVE",
  "verify": true
}
```

Lưu ý:
- `username` và `email` không được trùng.
- `password` áp dụng rule strong password.
- Trả về object `MemberResponse`.

### 4.4 Cập nhật user
- `PUT /api/admin/users/{id}`

Body (gửi field nào cần update):
```json
{
  "fullName": "Updated Name",
  "email": "updated@example.com",
  "roleName": "MANAGER",
  "status": "ACTIVE",
  "verify": true,
  "locked": false
}
```

### 4.5 Xóa user
- `DELETE /api/admin/users/{id}`

Response:
```json
{
  "statusCode": 200,
  "message": "User deleted successfully",
  "data": null
}
```

### 4.6 Lấy profile hiện tại
- `GET /api/admin/users/me`
- API này chỉ cần authenticated, không bắt buộc `USER_MANAGE`.

---

## 5. Role Management (`/api/admin/roles`)

Lưu ý bảo mật hiện tại: **không có create role / delete role**.

### 5.1 Lấy danh sách role
- `GET /api/admin/roles`
- Permission: `ROLE_VIEW`

### 5.2 Lấy role theo id
- `GET /api/admin/roles/{id}`
- Permission: `ROLE_VIEW`

### 5.3 Cập nhật tên role
- `PUT /api/admin/roles/{id}`
- Permission: `ROLE_UPDATE`

Body:
```json
{
  "name": "SUPERVISOR"
}
```

### 5.4 Cập nhật permission cho role
- `PUT /api/admin/roles/{id}/permissions`
- Permission: `ROLE_UPDATE_PERMISSIONS`

Body:
```json
{
  "permissionIds": [1, 2, 3]
}
```

---

## 6. Permission Management (`/api/admin/permissions`)

### 6.1 Lấy danh sách permission
- `GET /api/admin/permissions`
- Permission: `PERMISSION_VIEW`

### 6.2 Cập nhật tên permission
- `PUT /api/admin/permissions/{id}`
- Permission: `PERMISSION_UPDATE`

Body:
```json
{
  "name": "Permission Name Updated"
}
```

---

## 7. Catalog Management (`/api/admin/catalog`)

### 7.1 Stores

#### 7.1.1 Danh sách stores
- `GET /api/admin/catalog/stores?name=District&status=ACTIVE&page=0&size=20`
- Permission: `FRANCHISE_STORE_MANAGE`

#### 7.1.2 Chi tiết store
- `GET /api/admin/catalog/stores/{id}`
- Permission: `FRANCHISE_STORE_MANAGE`

#### 7.1.3 Tạo store
- `POST /api/admin/catalog/stores`
- Permission: `FRANCHISE_STORE_MANAGE`

Body:
```json
{
  "id": "ST010",
  "name": "Store District 10",
  "address": "10 Nguyen Trai, HCM",
  "phone": "0900000000",
  "manager": "Manager A",
  "status": "ACTIVE",
  "openDate": "2026-04-01"
}
```

#### 7.1.4 Cập nhật store
- `PUT /api/admin/catalog/stores/{id}`
- Permission: `FRANCHISE_STORE_MANAGE`

Body:
```json
{
  "name": "Store District 10 Updated",
  "address": "10 Nguyen Trai, HCM",
  "phone": "0900000001",
  "manager": "Manager B",
  "status": "ACTIVE",
  "openDate": "2026-04-02"
}
```

#### 7.1.5 Cập nhật status store
- `PATCH /api/admin/catalog/stores/{id}/status?status=INACTIVE`
- Permission: `FRANCHISE_STORE_MANAGE`

### 7.2 Kitchens

#### 7.2.1 Danh sách kitchens
- `GET /api/admin/catalog/kitchens?name=Central&status=ACTIVE&page=0&size=20`
- Permission: `CENTRAL_KITCHEN_MANAGE`

#### 7.2.2 Chi tiết kitchen
- `GET /api/admin/catalog/kitchens/{id}`
- Permission: `CENTRAL_KITCHEN_MANAGE`

#### 7.2.3 Tạo kitchen
- `POST /api/admin/catalog/kitchens`
- Permission: `CENTRAL_KITCHEN_MANAGE`

Body:
```json
{
  "id": "KIT010",
  "name": "Central Kitchen 10",
  "address": "100 Le Loi, HCM",
  "phone": "0911111111",
  "capacity": 500,
  "status": "ACTIVE"
}
```

#### 7.2.4 Cập nhật kitchen
- `PUT /api/admin/catalog/kitchens/{id}`
- Permission: `CENTRAL_KITCHEN_MANAGE`

Body:
```json
{
  "name": "Central Kitchen 10 Updated",
  "address": "100 Le Loi, HCM",
  "phone": "0911111112",
  "capacity": 600,
  "status": "ACTIVE"
}
```

#### 7.2.5 Cập nhật status kitchen
- `PATCH /api/admin/catalog/kitchens/{id}/status?status=INACTIVE`
- Permission: `CENTRAL_KITCHEN_MANAGE`

---

## 8. System Config (`/api/admin/system-config`)

Permission chung: `SYSTEM_CONFIG_MANAGE`

### 8.1 Lấy danh sách cấu hình priority
- `GET /api/admin/system-config/order-priority`

Response `data`:
```json
[
  {
    "id": 1,
    "priorityCode": "HIGH",
    "minDays": 0,
    "maxDays": 0,
    "description": "Gấp: Giao trong ngày"
  }
]
```

### 8.2 Tạo cấu hình priority
- `POST /api/admin/system-config/order-priority`

Body:
```json
{
  "priorityCode": "NORMAL",
  "minDays": 1,
  "maxDays": 2,
  "description": "Vừa: Giao trong 1-2 ngày"
}
```

### 8.3 Cập nhật cấu hình priority
- `PUT /api/admin/system-config/order-priority/{id}`

Body:
```json
{
  "priorityCode": "LOW",
  "minDays": 3,
  "maxDays": 5,
  "description": "Thấp: Giao trong 3-5 ngày"
}
```

Validation:
- `maxDays >= minDays`
- `priorityCode` chỉ nhận `HIGH|NORMAL|LOW`
- Nếu `fromDate > toDate` ở report sẽ trả lỗi 400 (xem mục reports)

---

## 9. Reports (`/api/admin/reports`)

### 9.1 System overview
- `GET /api/admin/reports/system-overview`
- `GET /api/admin/reports/system-overview?fromDate=2026-04-01&toDate=2026-04-30`
- Permission: `SYSTEM_REPORT_VIEW`

Query params:
- `fromDate` (optional): `yyyy-MM-dd`
- `toDate` (optional): `yyyy-MM-dd`

Ý nghĩa filter ngày:
- Filter áp dụng cho phần đơn hàng: `totalOrders` và `orderStatusCounts` (theo `orders.createdAt`).
- Nếu chỉ truyền `fromDate`: lấy từ ngày đó trở đi.
- Nếu chỉ truyền `toDate`: lấy đến hết ngày đó.
- Nếu `fromDate > toDate`: trả 400.

Response mẫu:
```json
{
  "statusCode": 200,
  "message": "System overview report retrieved successfully",
  "data": {
    "totalUsers": 12,
    "totalRoles": 6,
    "totalStores": 3,
    "activeStores": 2,
    "totalKitchens": 2,
    "activeKitchens": 2,
    "totalProducts": 25,
    "totalOrders": 120,
    "orderStatusCounts": {
      "PENDING": 10,
      "ASSIGNED": 8,
      "IN_PROGRESS": 7,
      "PACKED_WAITING_SHIPPER": 3,
      "SHIPPING": 6,
      "DELIVERED": 80,
      "CANCELLED": 2,
      "PROCESSING": 2,
      "APPROVED": 2
    }
  }
}
```

---

## 10. Lỗi thường gặp cho FE

### 400 - Bad Request
Ví dụ:
- `fromDate` lớn hơn `toDate`
- Status truyền sai enum
- Payload thiếu field bắt buộc

### 401 - Unauthorized
- Không có token
- Token hết hạn

### 403 - Forbidden
- Token hợp lệ nhưng thiếu permission (`USER_MANAGE`, `SYSTEM_REPORT_VIEW`, ...)

### 404 - Not Found
- User/Role/Store/Kitchen/Config id không tồn tại

### 409 - Conflict
- Username hoặc email bị trùng
- Priority code bị trùng
