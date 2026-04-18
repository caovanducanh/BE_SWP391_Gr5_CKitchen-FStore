# Store Staff API Integration Guide (FE)

Tài liệu này dành cho Frontend tích hợp API cho role `FRANCHISE_STORE_STAFF`.

Mục tiêu: FE có thể nối API nhanh, đúng luồng nghiệp vụ, biết rõ request/response, trạng thái và lỗi thường gặp.

---

## 1) Tổng quan

- **Prefix API Store Staff**: `/api/store`
- **Content-Type**: `application/json`
- **Auth**: JWT Bearer token trong header:
  - `Authorization: Bearer <access_token>`

> Lưu ý: toàn bộ API trong tài liệu này yêu cầu user đăng nhập và có quyền tương ứng của Store Staff.

---

## 2) Quy ước response (cực kỳ quan trọng cho FE)

Backend đang dùng `@ApiResponse`, vì vậy response thành công theo format:

```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

### 2.1) HTTP status thành công
- `GET`: thường `200`
- `POST`: thường `201` (vì `@PostMapping`)

### 2.2) Response lỗi chuẩn
Khi lỗi, backend trả:

```json
{
  "statusCode": 400,
  "message": "...",
  "data": null
}
```

Hoặc với lỗi validation body:

```json
{
  "statusCode": 400,
  "message": "Validation failed",
  "data": {
    "fieldName": "error message"
  }
}
```

### 2.3) API phân trang
Các API list có `page` + `size` và nằm trong `data`.

Ở tầng controller, API được đánh dấu phân trang (`@PageResponse`) và trả object phân trang. FE nên đọc theo các key phổ biến:
- `content`
- `pageNumber`
- `pageSize`
- `totalElements`
- `totalPages`
- `last`

---

## 3) Enum/giá trị chuẩn FE phải dùng

## 3.1) OrderStatus
Danh sách status backend hỗ trợ (trả về qua API statuses):

- `PENDING`
- `ASSIGNED`
- `IN_PROGRESS`
- `PACKED_WAITING_SHIPPER`
- `SHIPPING`
- `DELIVERED`
- `CANCELLED`
- `PROCESSING` *(legacy)*
- `APPROVED` *(legacy)*

> FE nên ưu tiên hiển thị 7 status chính đầu tiên cho flow mới.

### 3.2) ProductCategory
- `BAKERY`
- `BEVERAGE`
- `SNACK`
- `FROZEN`
- `OTHER`

### 3.3) Delivery status (dùng cho filter)
Backend hiện dùng string status cho Delivery:
- `ASSIGNED`
- `SHIPPING`
- `DELIVERED`

---

## 4) Luồng tích hợp khuyến nghị cho FE

1. Login lấy access token.
2. Màn hình dashboard: gọi `GET /api/store/overview`.
3. Màn hình tạo đơn:
   - gọi `GET /api/store/products` để chọn hàng.
   - gửi `POST /api/store/orders`.
4. Màn hình quản lý đơn:
   - gọi `GET /api/store/orders` (lọc status nếu cần).
   - gọi `GET /api/store/orders/{orderId}` xem chi tiết.
   - gọi `GET /api/store/orders/{orderId}/timeline` hiển thị tiến trình.
5. Màn hình giao hàng:
   - gọi `GET /api/store/deliveries`.
   - khi nhận hàng xong: `POST /api/store/deliveries/{deliveryId}/confirm`.
6. Màn hình tồn kho:
   - gọi `GET /api/store/inventory`.
7. Màn hình profile cửa hàng:
   - gọi `GET /api/store/my-store`.

---

## 5) Danh sách endpoint chi tiết

## 5.1) Tạo đơn hàng

- **Method**: `POST`
- **URL**: `/api/store/orders`
- **Permission**: `ORDER_CREATE`
- **Body**:

```json
{
  "requestedDate": "2026-04-20",
  "notes": "Giao trước 10h sáng",
  "items": [
    {
      "productId": "PROD001",
      "quantity": 10
    },
    {
      "productId": "PROD002",
      "quantity": 5
    }
  ]
}
```

### Validate request
- `requestedDate`: bắt buộc
- `items`: bắt buộc, không rỗng
- `items[].productId`: bắt buộc
- `items[].quantity`: bắt buộc, `>= 1`, `<= 9,999,999`

### Response thành công (`201`)
`data` là `OrderResponse`:

```json
{
  "statusCode": 201,
  "message": "Order created successfully",
  "data": {
    "id": "ORD0418001",
    "storeId": "ST001",
    "storeName": "Store 1",
    "kitchenId": null,
    "kitchenName": null,
    "status": "PENDING",
    "priority": "NORMAL",
    "createdAt": "2026-04-18T21:00:00",
    "requestedDate": "2026-04-20",
    "notes": "Giao trước 10h sáng",
    "createdBy": "store_staff_01",
    "total": 250000,
    "updatedAt": "2026-04-18T21:00:00",
    "items": [
      {
        "id": 1,
        "productId": "PROD001",
        "productName": "Bread",
        "quantity": 10,
        "unit": "piece",
        "createdAt": "2026-04-18T21:00:00"
      }
    ]
  }
}
```

### Lỗi thường gặp
- `404 Product not found: <id>`
- `400 Validation failed`
- `403 Permission denied`

---

## 5.2) Danh sách đơn hàng

- **Method**: `GET`
- **URL**: `/api/store/orders`
- **Permission**: `ORDER_VIEW`
- **Query**:
  - `status` *(optional)*: ví dụ `PENDING`, `SHIPPING`...
  - `page` *(default=0)*
  - `size` *(default=20)*

### Ví dụ
`GET /api/store/orders?status=SHIPPING&page=0&size=20`

### Response thành công (`200`)
`data` là object phân trang, `content` gồm `OrderResponse[]`.

---

## 5.3) Chi tiết đơn hàng

- **Method**: `GET`
- **URL**: `/api/store/orders/{orderId}`
- **Permission**: `ORDER_VIEW`

### Response
`data` là `OrderResponse` (full items).

### Lỗi
- `404 Order not found: {orderId}`

---

## 5.4) Timeline trạng thái đơn hàng

- **Method**: `GET`
- **URL**: `/api/store/orders/{orderId}/timeline`
- **Permission**: `ORDER_VIEW`

### Response (`200`)
`data` là `OrderTimelineResponse`:

```json
{
  "statusCode": 200,
  "message": "Order timeline retrieved successfully",
  "data": {
    "orderId": "ORD0418001",
    "currentStatus": "IN_PROGRESS",
    "createdAt": "2026-04-18T10:00:00",
    "assignedAt": "2026-04-18T10:10:00",
    "inProgressAt": "2026-04-18T10:20:00",
    "packedWaitingShipperAt": null,
    "shippingAt": null,
    "deliveredAt": null,
    "cancelledAt": null,
    "updatedAt": "2026-04-18T10:20:00"
  }
}
```

### FE mapping gợi ý
- Mốc chưa diễn ra => `null`
- Hiển thị stepper theo thứ tự:
  `PENDING` -> `ASSIGNED` -> `IN_PROGRESS` -> `PACKED_WAITING_SHIPPER` -> `SHIPPING` -> `DELIVERED`
- Nếu `CANCELLED`, hiển thị nhánh hủy theo `cancelledAt`.

---

## 5.5) Danh sách trạng thái đơn hàng (cho filter/badge)

- **Method**: `GET`
- **URL**: `/api/store/orders/statuses`
- **Permission**: `ORDER_VIEW`

### Response
`data` là `string[]` gồm toàn bộ status enum backend.

Ví dụ:

```json
{
  "statusCode": 200,
  "message": "Order statuses retrieved successfully",
  "data": [
    "PENDING",
    "ASSIGNED",
    "IN_PROGRESS",
    "PACKED_WAITING_SHIPPER",
    "SHIPPING",
    "DELIVERED",
    "CANCELLED",
    "PROCESSING",
    "APPROVED"
  ]
}
```

---

## 5.6) Danh sách delivery của cửa hàng

- **Method**: `GET`
- **URL**: `/api/store/deliveries`
- **Permission**: `DELIVERY_VIEW`
- **Query**:
  - `status` *(optional)*: `ASSIGNED | SHIPPING | DELIVERED`
  - `page` *(default=0)*
  - `size` *(default=20)*

### Ví dụ
`GET /api/store/deliveries?status=SHIPPING&page=0&size=20`

### Response
`data` là phân trang `DeliveryResponse[]`.

`DeliveryResponse` fields:
- `id`
- `orderId`
- `coordinatorName`
- `status`
- `assignedAt`
- `deliveredAt`
- `notes`
- `receiverName`
- `temperatureOk`
- `createdAt`
- `updatedAt`

> Lưu ý: API cũ `GET /api/store/orders/{orderId}/delivery` đã được bỏ, FE không dùng endpoint này nữa.

---

## 5.7) Xác nhận nhận hàng

- **Method**: `POST`
- **URL**: `/api/store/deliveries/{deliveryId}/confirm`
- **Permission**: `DELIVERY_CONFIRM`
- **Body**:

```json
{
  "receiverName": "Nguyen Van A",
  "temperatureOk": true,
  "notes": "Hàng đủ, bao bì ổn"
}
```

### Validate
- `receiverName`: bắt buộc
- `temperatureOk`: bắt buộc
- `notes`: optional

### Response
`data` là `DeliveryResponse` đã cập nhật sang `DELIVERED`.

### Side effect nghiệp vụ
- Delivery status -> `DELIVERED`
- Order status -> `DELIVERED`

---

## 5.8) Xem tồn kho cửa hàng

- **Method**: `GET`
- **URL**: `/api/store/inventory`
- **Permission**: `STORE_INVENTORY_VIEW`
- **Query**:
  - `productId` *(optional)*
  - `productName` *(optional, tìm gần đúng)*
  - `page` *(default=0)*
  - `size` *(default=20)*

### Response
`data` là phân trang `StoreInventoryResponse[]`.

`StoreInventoryResponse`:
- `id`
- `storeId`
- `storeName`
- `productId`
- `productName`
- `quantity`
- `unit`
- `minStock`
- `expiryDate`
- `updatedAt`
- `lowStock` (backend tự tính `quantity <= minStock`)

### Lỗi
- Nếu truyền `productId` không tồn tại: `404 Product not found: <id>`

---

## 5.9) Lấy thông tin cửa hàng hiện tại

- **Method**: `GET`
- **URL**: `/api/store/my-store`
- **Permission**: `STORE_VIEW`

### Response
`data` là `StoreResponse`:
- `id`, `name`, `address`, `phone`, `manager`, `status`, `openDate`, `createdAt`, `updatedAt`

---

## 5.10) Tổng quan vận hành cửa hàng

- **Method**: `GET`
- **URL**: `/api/store/overview`
- **Permission**: `STORE_VIEW`

### Response
`data` là `StoreOverviewResponse`:

- `storeId`
- `storeName`
- `totalOrders`
- `pendingOrders`
- `inProgressOrders`
- `shippingOrders`
- `deliveredOrders`
- `cancelledOrders`
- `lowStockItems`
- `activeDeliveries`

`activeDeliveries` hiện được tính theo delivery status: `ASSIGNED` + `SHIPPING`.

---

## 5.11) Danh sách sản phẩm có thể đặt

- **Method**: `GET`
- **URL**: `/api/store/products`
- **Permission**: `ORDER_CREATE`
- **Query**:
  - `name` *(optional, partial match)*
  - `category` *(optional: `BAKERY | BEVERAGE | SNACK | FROZEN | OTHER`)*
  - `page` *(default=0)*
  - `size` *(default=20)*

### Response
`data` là phân trang `ProductResponse[]`.

`ProductResponse`:
- `id`
- `name`
- `category`
- `unit`
- `price`
- `cost`
- `imageUrl` (`string[]`)
- `createdAt`
- `updatedAt`

### Lỗi
- `400 Invalid product category: <value>`

---

## 6) Permission mapping theo endpoint

| Endpoint | Permission |
|---|---|
| `POST /api/store/orders` | `ORDER_CREATE` |
| `GET /api/store/orders` | `ORDER_VIEW` |
| `GET /api/store/orders/{orderId}` | `ORDER_VIEW` |
| `GET /api/store/orders/{orderId}/timeline` | `ORDER_VIEW` |
| `GET /api/store/orders/statuses` | `ORDER_VIEW` |
| `GET /api/store/deliveries` | `DELIVERY_VIEW` |
| `POST /api/store/deliveries/{deliveryId}/confirm` | `DELIVERY_CONFIRM` |
| `GET /api/store/inventory` | `STORE_INVENTORY_VIEW` |
| `GET /api/store/my-store` | `STORE_VIEW` |
| `GET /api/store/overview` | `STORE_VIEW` |
| `GET /api/store/products` | `ORDER_CREATE` |

---

## 7) FE checklist để nối nhanh và ít lỗi

- [ ] Luôn gửi `Authorization: Bearer <token>`.
- [ ] Parse response theo `statusCode/message/data`.
- [ ] Với list API, đọc phân trang từ `data` (`content`, `pageNumber`, `totalElements`...).
- [ ] Dùng `GET /api/store/orders/statuses` để render dropdown filter status.
- [ ] Timeline phải handle `null timestamp`.
- [ ] Màn hình confirm receipt bắt buộc `receiverName`, `temperatureOk`.
- [ ] Không gọi endpoint đã bỏ: `/api/store/orders/{orderId}/delivery`.

---

## 8) Gợi ý typing (TypeScript)

```ts
export type ApiEnvelope<T> = {
  statusCode: number;
  message: string;
  data: T;
};

export type Paged<T> = {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
};

export type OrderStatus =
  | "PENDING"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "PACKED_WAITING_SHIPPER"
  | "SHIPPING"
  | "DELIVERED"
  | "CANCELLED"
  | "PROCESSING"
  | "APPROVED";
```

---

## 9) Chú ý nghiệp vụ thực tế

- API store chủ yếu tự suy ra store từ user đăng nhập (không truyền `storeId` từ FE).
- `priority` của order backend tự tính theo `requestedDate`.
- Khi confirm delivery, order chuyển `DELIVERED` ngay.
- Timeline giúp FE hiển thị trải nghiệm tracking kiểu stepper.

---

Nếu cần, mình có thể viết thêm bản “FE-ready” gồm luôn:
- collection JSON cho Postman,
- sample service code Axios,
- mapping trạng thái sang label tiếng Việt + màu UI.
