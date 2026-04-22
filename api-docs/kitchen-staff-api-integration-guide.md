# Central Kitchen Staff API Integration Guide (FE)

Tài liệu này dành cho Frontend tích hợp API cho role `CENTRAL_KITCHEN_STAFF`.

Mục tiêu: FE nối API nhanh, đúng flow vận hành bếp trung tâm, giảm lỗi mapping/status khi lên UI.

---

## 1) Tổng quan

- **Prefix API Kitchen Staff**: `/api/central-kitchen`
- **Content-Type**: `application/json`
- **Auth**: JWT Bearer token trong header
  - `Authorization: Bearer <access_token>`

> Tất cả endpoint trong tài liệu này yêu cầu đăng nhập + đúng permission.

---

## 2) Quy ước response chuẩn (áp dụng toàn bộ API)

Backend wrap response bằng `@ApiResponse`, format thành công:

```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

### 2.1) HTTP status thành công
- `GET`: thường `200`
- `PATCH`: `200`
- `POST`: thường `201`

### 2.2) Response lỗi
Mẫu lỗi phổ biến:

```json
{
  "statusCode": 400,
  "message": "...",
  "data": null
}
```

Validation body:

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
Các API list có `page` + `size`, `data` chứa object phân trang:
- `content`
- `pageNumber`
- `pageSize`
- `totalElements`
- `totalPages`
- `last`

---

## 3) Enum/trạng thái FE phải dùng đúng

## 3.1) OrderStatus toàn hệ thống
- `PENDING`
- `ASSIGNED`
- `IN_PROGRESS`
- `PACKED_WAITING_SHIPPER`
- `SHIPPING`
- `DELIVERED`
- `CANCELLED`
- `PROCESSING` *(legacy)*
- `APPROVED` *(legacy)*

## 3.2) Danh sách status cho UI cập nhật ở kitchen
API `GET /api/central-kitchen/order-statuses` trả đúng tập status dành cho nút cập nhật trạng thái:
- `IN_PROGRESS`
- `PACKED_WAITING_SHIPPER`
- `SHIPPING`
- `DELIVERED`
- `CANCELLED`

> `ASSIGNED` không update qua API status; trạng thái này được set bằng API assign.

## 3.3) Rule chuyển trạng thái (quan trọng)
Backend có chặn transition. FE nên follow đúng thứ tự:

- `PENDING` -> `ASSIGNED` / `IN_PROGRESS` / `CANCELLED`
- `ASSIGNED` -> `IN_PROGRESS` / `CANCELLED`
- `IN_PROGRESS` -> `PACKED_WAITING_SHIPPER` / `CANCELLED`
- `PACKED_WAITING_SHIPPER` -> `SHIPPING` / `CANCELLED`
- `SHIPPING` -> `DELIVERED`
- Legacy:
  - `PROCESSING` -> `PACKED_WAITING_SHIPPER` / `SHIPPING` / `CANCELLED`
  - `APPROVED` -> `IN_PROGRESS` / `SHIPPING` / `CANCELLED`

Nếu sai rule sẽ nhận lỗi:
- `400 Invalid status transition from <OLD> to <NEW>`

---

## 4) Flow tích hợp FE khuyến nghị

1. Login lấy access token.
2. Dashboard kitchen: `GET /api/central-kitchen/overview`.
3. Danh sách đơn cần xử lý: `GET /api/central-kitchen/orders`.
4. Nhấn “tiếp nhận đơn”: `PATCH /api/central-kitchen/orders/{orderId}/assign`.
5. Cập nhật trạng thái theo tiến trình: `PATCH /api/central-kitchen/orders/{orderId}/status`.
6. Nhập lô nguyên liệu: `POST /api/central-kitchen/ingredient-batches`.
7. Theo dõi lô nguyên liệu + tồn kho tổng hợp:
  - `GET /api/central-kitchen/ingredient-batches`
  - `GET /api/central-kitchen/inventory`
8. Quản lý kế hoạch sản xuất:
  - list: `GET /api/central-kitchen/production-plans`
  - detail: `GET /api/central-kitchen/production-plans/{planId}`
  - tạo: `POST /api/central-kitchen/production-plans`
  - bắt đầu: `PATCH /api/central-kitchen/production-plans/{planId}/start`
  - hoàn tất: `PATCH /api/central-kitchen/production-plans/{planId}/complete`
  - hủy: `PATCH /api/central-kitchen/production-plans/{planId}/cancel`
9. Theo dõi lô thành phẩm:
  - `GET /api/central-kitchen/product-batches`
  - `GET /api/central-kitchen/product-batches/{batchId}`
10. Xem thông tin bếp hiện tại: `GET /api/central-kitchen/my-kitchen`.
11. Xem danh sách cửa hàng để điều phối: `GET /api/central-kitchen/stores`.

---

## 5) Danh sách endpoint chi tiết

## 5.1) Danh sách đơn hàng từ cửa hàng

- **Method**: `GET`
- **URL**: `/api/central-kitchen/orders`
- **Permission**: `ORDER_VIEW`
- **Query**:
  - `status` *(optional)*
  - `storeId` *(optional)*
  - `page` *(default=0)*
  - `size` *(default=20)*

### Ví dụ
`GET /api/central-kitchen/orders?status=PENDING&storeId=ST001&page=0&size=20`

### Response
`data` là phân trang `OrderResponse[]`.

### Lỗi thường gặp
- `400 Invalid order status: <value>`
- `404 Store not found: <storeId>`

---

## 5.2) Chi tiết đơn hàng

- **Method**: `GET`
- **URL**: `/api/central-kitchen/orders/{orderId}`
- **Permission**: `ORDER_VIEW`

### Response
`data` là `OrderResponse` (có đầy đủ `items`).

### Lỗi
- `404 Order not found: {orderId}`

---

## 5.3) Tiếp nhận/gán đơn hàng vào bếp của user đăng nhập

- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/orders/{orderId}/assign`
- **Permission**: `ORDER_ASSIGN`
- **Body**: không có

### Ý nghĩa nghiệp vụ
- Không truyền `kitchenId`.
- Backend tự lấy kitchen từ user đăng nhập.
- Đặt status thành `ASSIGNED`.
- Set `assignedAt` nếu trước đó chưa có.

### Response
`data` là `OrderResponse` sau khi assign.

### Lỗi thường gặp
- `400 Central kitchen staff is not assigned to any kitchen`
- `404 Order not found: {orderId}`
- `400 Order cannot be assigned from status: <status hiện tại>`

---

## 5.4) Cập nhật trạng thái đơn hàng

- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/orders/{orderId}/status`
- **Permission**: `ORDER_STATUS_UPDATE`
- **Body**:

```json
{
  "status": "IN_PROGRESS",
  "notes": "Bắt đầu sản xuất"
}
```

### Validate body
- `status`: bắt buộc
- `notes`: optional, có thể null
- Status FE nên dùng từ API `/order-statuses`

### Response
`data` là `OrderResponse` đã cập nhật.

### Hành vi backend cần FE biết
- Nếu `status` đổi hợp lệ, backend cập nhật các timestamp milestone tương ứng (`inProgressAt`, `shippingAt`, `deliveredAt`...).
- Nếu `notes` có giá trị, backend append vào `notes` hiện có theo format internal log `[CK username - yyyy-MM-dd HH:mm] ...`.

### Lỗi thường gặp
- `404 Order not found: {orderId}`
- `400 Invalid order status: <value>`
- `400 Invalid status transition from <OLD> to <NEW>`

---

## 5.5) Danh sách status dành cho màn hình cập nhật trạng thái

- **Method**: `GET`
- **URL**: `/api/central-kitchen/order-statuses`
- **Permission**: `ORDER_STATUS_UPDATE`

### Response ví dụ

```json
{
  "statusCode": 200,
  "message": "Order statuses retrieved successfully",
  "data": [
    "IN_PROGRESS",
    "PACKED_WAITING_SHIPPER",
    "SHIPPING",
    "DELIVERED",
    "CANCELLED"
  ]
}
```

---

## 5.6) Danh sách kế hoạch sản xuất

- **Method**: `GET`
- **URL**: `/api/central-kitchen/production-plans`
- **Permission**: `PRODUCTION_PLAN_VIEW`
- **Query**:
  - `page` *(default=0)*
  - `size` *(default=20)*

### Response
`data` là phân trang `ProductionPlanResponse[]`.

`ProductionPlanResponse` fields:
- `id`
- `productId`
- `productName`
- `kitchenId`
- `kitchenName`
- `quantity`
- `unit`
- `status`
- `startDate`
- `endDate`
- `staff`
- `notes`
- `createdAt`
- `updatedAt`
- `ingredients` *(có thể có, đặc biệt khi gọi detail)*

---

## 5.7) Chi tiết kế hoạch sản xuất

- **Method**: `GET`
- **URL**: `/api/central-kitchen/production-plans/{planId}`
- **Permission**: `PRODUCTION_PLAN_VIEW`

### Response
`data` là `ProductionPlanResponse`.

Trong response detail, `ingredients` gồm:
- `ingredientId`
- `ingredientName`
- `requiredQuantity`
- `availableQuantity`
- `unit`
- `sufficient`

### Lỗi thường gặp
- `404 Production plan not found: {planId}`
- `400 Plan does not belong to your kitchen`

---

## 5.8) Tạo kế hoạch sản xuất

- **Method**: `POST`
- **URL**: `/api/central-kitchen/production-plans`
- **Permission**: `PRODUCTION_PLAN_CREATE`
- **Body**:

```json
{
  "productId": "PROD001",
  "quantity": 100,
  "startDate": "2026-04-19T08:00:00",
  "endDate": "2026-04-19T17:00:00",
  "notes": "Ưu tiên cho đợt cao điểm"
}
```

### Validate
- `productId`: bắt buộc
- `quantity`: bắt buộc, `>= 1`
- `startDate`: bắt buộc
- `endDate`: bắt buộc
- Rule nghiệp vụ: `endDate` phải sau `startDate`

### Response
`data` là `ProductionPlanResponse` vừa tạo (`status` mặc định backend là `DRAFT`).

### Lỗi thường gặp
- `400 endDate must be after startDate`
- `404 Product not found: <productId>`

---

---

## 5.9) Bắt đầu kế hoạch sản xuất (trừ kho thực tế)

- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/production-plans/{planId}/start`
- **Permission**: `PRODUCTION_PLAN_UPDATE`
- **Body**: không có

### Rule nghiệp vụ
- Chỉ được start khi plan đang `DRAFT` hoặc `APPROVED`.
- Nếu tồn kho thực tế tại lô đã thay đổi, backend có thể từ chối và yêu cầu tạo lại plan.

### Lỗi thường gặp
- `404 Plan not found: {planId}`
- `400 Can only start DRAFT or APPROVED plans`
- `400 Batch <batchNo> does not have enough qty now. Please recreate plan.`

---

## 5.10) Hoàn tất kế hoạch sản xuất (tạo lô thành phẩm)

- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/production-plans/{planId}/complete`
- **Permission**: `PRODUCTION_PLAN_UPDATE`
- **Body**:

```json
{
  "notes": "Hoàn tất ca sáng",
  "expiryDate": "2026-04-30"
}
```

### Rule nghiệp vụ
- Chỉ complete khi plan đang `IN_PRODUCTION`.
- `expiryDate` bắt buộc.
- Backend tự tạo lô thành phẩm mới với status `AVAILABLE`.

### Lỗi thường gặp
- `404 Plan not found: {planId}`
- `400 Plan is not in production`
- `400 expiryDate is required for the finished product batch`

---

## 5.11) Hủy kế hoạch sản xuất

- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/production-plans/{planId}/cancel`
- **Permission**: `PRODUCTION_PLAN_UPDATE`
- **Body**:

```json
{
  "notes": "Hủy do đổi kế hoạch"
}
```

### Rule nghiệp vụ
- Không thể hủy plan đã `COMPLETED`.
- Nếu plan đang `IN_PRODUCTION`, backend tự hoàn trả nguyên liệu đã trừ về đúng lô ban đầu.

### Lỗi thường gặp
- `404 Plan not found: {planId}`
- `400 Cannot cancel completed plan`

---

## 5.12) Nhập lô nguyên liệu mới

- **Method**: `POST`
- **URL**: `/api/central-kitchen/ingredient-batches`
- **Permission**: `KITCHEN_INVENTORY_CREATE`
- **Body**:

```json
{
  "ingredientId": "ING001",
  "batchNo": "SUP-LOT-20260423-01",
  "quantity": 120.5,
  "expiryDate": "2026-05-15",
  "supplier": "ABC Supplier",
  "importPrice": 350000,
  "importDate": "2026-04-23",
  "notes": "Lô nhập sáng",
  "minStock": 20
}
```

### Validate quan trọng
- `ingredientId`: bắt buộc
- `batchNo`: bắt buộc, max 30
- `quantity`: bắt buộc, > 0
- `expiryDate`: bắt buộc

### Lỗi thường gặp
- `400 Số lô '...' đã tồn tại cho nguyên liệu này trong bếp`
- `400 Ngày hết hạn không được nhỏ hơn ngày hiện tại`
- `404 Không tìm thấy nguyên liệu: ...`

---

## 5.13) Danh sách lô nguyên liệu

- **Method**: `GET`
- **URL**: `/api/central-kitchen/ingredient-batches`
- **Permission**: `KITCHEN_INVENTORY_VIEW`
- **Query**:
  - `ingredientId` *(optional)*
  - `ingredientName` *(optional)*
  - `status` *(optional)*
  - `page` *(default=0)*
  - `size` *(default=20)*

### Response
`data` là phân trang `IngredientBatchResponse[]`.

`IngredientBatchResponse` fields:
- `id`
- `kitchenId`
- `kitchenName`
- `ingredientId`
- `ingredientName`
- `batchNo`
- `initialQuantity`
- `remainingQuantity`
- `unit`
- `expiryDate`
- `supplier`
- `importPrice`
- `importDate`
- `status`
- `notes`
- `nearExpiry`
- `createdAt`
- `updatedAt`

---

## 5.14) Chi tiết 1 lô nguyên liệu

- **Method**: `GET`
- **URL**: `/api/central-kitchen/ingredient-batches/{id}`
- **Permission**: `KITCHEN_INVENTORY_VIEW`

### Lỗi thường gặp
- `404 Không tìm thấy lô nguyên liệu: {id}`
- `400 Lô này không thuộc bếp của bạn`

---

## 5.15) Xem tồn kho nguyên liệu bếp (tổng hợp + chi tiết lô)

- **Method**: `GET`
- **URL**: `/api/central-kitchen/inventory`
- **Permission**: `KITCHEN_INVENTORY_VIEW`
- **Query**:
  - `ingredientId` *(optional)*
  - `ingredientName` *(optional, contains + ignore case)*
  - `lowStock` *(optional: true/false)*
  - `page` *(default=0)*
  - `size` *(default=20)*

### Response
`data` là phân trang `KitchenInventoryDetailResponse[]`.

`KitchenInventoryDetailResponse` fields:
- `id`
- `kitchenId`
- `kitchenName`
- `ingredientId`
- `ingredientName`
- `totalQuantity`
- `unit`
- `minStock`
- `lowStock` (backend tính từ `totalQuantity <= minStock`)
- `updatedAt`
- `batches`: `IngredientBatchResponse[]` (danh sách lô active theo FEFO)

---

## 5.16) Danh sách lô thành phẩm

- **Method**: `GET`
- **URL**: `/api/central-kitchen/product-batches`
- **Permission**: `PRODUCTION_PLAN_VIEW`
- **Query**:
  - `productId` *(optional)*
  - `status` *(optional)*
  - `page` *(default=0)*
  - `size` *(default=20)*

### Response
`data` là phân trang `BatchResponse[]`.

`BatchResponse` fields:
- `id`
- `planId`
- `productId`
- `productName`
- `kitchenId`
- `kitchenName`
- `quantity`
- `remainingQuantity`
- `unit`
- `expiryDate`
- `status`
- `staff`
- `notes`
- `createdAt`
- `updatedAt`
- `ingredientBatchUsages` *(traceability)*

---

## 5.17) Chi tiết lô thành phẩm

- **Method**: `GET`
- **URL**: `/api/central-kitchen/product-batches/{batchId}`
- **Permission**: `PRODUCTION_PLAN_VIEW`

### Lỗi thường gặp
- `404 Batch not found: {batchId}`
- `400 Batch does not belong to your kitchen`
- **URL**: `/api/central-kitchen/my-kitchen`
- **Permission**: `KITCHEN_INVENTORY_VIEW`

## 5.18) Thông tin bếp hiện tại của user
`data` là `KitchenResponse`:
- `id`, `name`, `address`, `phone`, `capacity`, `status`, `createdAt`, `updatedAt`

### Lỗi
- `400 Central kitchen staff is not assigned to any kitchen`

---

## 5.19) Tổng quan vận hành bếp

- **Method**: `GET`
- **URL**: `/api/central-kitchen/overview`
- **Permission**: `ORDER_VIEW`
- **Query**:
  - `fromDate` *(optional, yyyy-MM-dd)*
  - `toDate` *(optional, yyyy-MM-dd)*

### Ví dụ
`GET /api/central-kitchen/overview?fromDate=2026-04-01&toDate=2026-04-30`

### Response
`data` là `CentralKitchenOverviewResponse`:
- `kitchenId`
- `kitchenName`
- `pendingUnassignedOrders`
- `assignedToMyKitchen`
- `inProgressOrders`
- `packedWaitingShipperOrders`
- `shippingOrders`
- `overdueOrders`

### Lỗi thường gặp
- `400 Central kitchen staff is not assigned to any kitchen`
- `400 fromDate must be before or equal to toDate`

---

## 5.20) Danh sách cửa hàng

- **Method**: `GET`
- **URL**: `/api/central-kitchen/stores`
- **Permission**: `STORE_VIEW`
- **Query**:
  - `name` *(optional, contains + ignore case)*
  - `status` *(optional, so khớp theo status store)*
  - `page` *(default=0)*
  - `size` *(default=20)*

### Ví dụ
`GET /api/central-kitchen/stores?name=district&status=ACTIVE&page=0&size=20`

### Response
`data` là phân trang `StoreResponse[]`.

---

## 6) Permission mapping theo endpoint

| Endpoint | Permission |
|---|---|
| `GET /api/central-kitchen/orders` | `ORDER_VIEW` |
| `GET /api/central-kitchen/orders/{orderId}` | `ORDER_VIEW` |
| `PATCH /api/central-kitchen/orders/{orderId}/assign` | `ORDER_ASSIGN` |
| `PATCH /api/central-kitchen/orders/{orderId}/status` | `ORDER_STATUS_UPDATE` |
| `GET /api/central-kitchen/order-statuses` | `ORDER_STATUS_UPDATE` |
| `GET /api/central-kitchen/production-plans` | `PRODUCTION_PLAN_VIEW` |
| `GET /api/central-kitchen/production-plans/{planId}` | `PRODUCTION_PLAN_VIEW` |
| `POST /api/central-kitchen/production-plans` | `PRODUCTION_PLAN_CREATE` |
| `PATCH /api/central-kitchen/production-plans/{planId}/start` | `PRODUCTION_PLAN_UPDATE` |
| `PATCH /api/central-kitchen/production-plans/{planId}/complete` | `PRODUCTION_PLAN_UPDATE` |
| `PATCH /api/central-kitchen/production-plans/{planId}/cancel` | `PRODUCTION_PLAN_UPDATE` |
| `POST /api/central-kitchen/ingredient-batches` | `KITCHEN_INVENTORY_CREATE` |
| `GET /api/central-kitchen/ingredient-batches` | `KITCHEN_INVENTORY_VIEW` |
| `GET /api/central-kitchen/ingredient-batches/{id}` | `KITCHEN_INVENTORY_VIEW` |
| `GET /api/central-kitchen/inventory` | `KITCHEN_INVENTORY_VIEW` |
| `GET /api/central-kitchen/product-batches` | `PRODUCTION_PLAN_VIEW` |
| `GET /api/central-kitchen/product-batches/{batchId}` | `PRODUCTION_PLAN_VIEW` |
| `GET /api/central-kitchen/my-kitchen` | `KITCHEN_INVENTORY_VIEW` |
| `GET /api/central-kitchen/overview` | `ORDER_VIEW` |
| `GET /api/central-kitchen/stores` | `STORE_VIEW` |

---

## 7) FE checklist để nối nhanh, hạn chế bug

- [ ] Luôn gửi `Authorization: Bearer <token>`.
- [ ] Parse response theo `statusCode/message/data`.
- [ ] Với list API: đọc `content/pageNumber/totalElements/...` trong `data`.
- [ ] Luôn lấy status update options từ `GET /api/central-kitchen/order-statuses`.
- [ ] Không hardcode transition trái rule.
- [ ] API assign không truyền `kitchenId`.
- [ ] Khi update status, cho phép `notes` rỗng hoặc không gửi.
- [ ] Start/complete/cancel production plan phải dùng `PATCH`.
- [ ] Tạo production plan xong status ban đầu là `DRAFT`.
- [ ] Nhập lô nguyên liệu dùng `/api/central-kitchen/ingredient-batches` (không dùng path cũ).
- [ ] API tồn kho hỗ trợ `lowStock=true/false`; cần truyền đúng kiểu boolean ở query.
- [ ] UI lô thành phẩm nên hiển thị traceability từ `ingredientBatchUsages`.
- [ ] Màn overview cần xử lý filter date optional.

---

## 8) TypeScript typing gợi ý

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

export type KitchenUpdateOrderStatus =
  | "IN_PROGRESS"
  | "PACKED_WAITING_SHIPPER"
  | "SHIPPING"
  | "DELIVERED"
  | "CANCELLED";

export type ProductionPlanStatus =
  | "DRAFT"
  | "APPROVED"
  | "IN_PRODUCTION"
  | "COMPLETED"
  | "CANCELLED";

export type IngredientBatchStatus =
  | "ACTIVE"
  | "DEPLETED"
  | "EXPIRED"
  | "DISPOSED";

export type ProductBatchStatus =
  | "AVAILABLE"
  | "PARTIALLY_DISTRIBUTED"
  | "FULLY_DISTRIBUTED";
```

---

## 9) Lưu ý nghiệp vụ quan trọng

- Store tạo đơn ở trạng thái `PENDING`.
- Kitchen staff dùng API assign để nhận đơn (`ASSIGNED`).
- Sau đó cập nhật theo flow vận hành tới `DELIVERED`.
- Trường `notes` của order có thể chứa nhiều dòng log nội bộ (append), FE nên hiển thị multiline.
- `overdueOrders` trong overview là đơn active có `requestedDate < hôm nay`.
- Tạo production plan chỉ reserve theo FEFO, chưa trừ kho thực tế.
- Chỉ khi gọi `start` thì mới trừ kho lô nguyên liệu và trừ tồn kho tổng.
- Nếu hủy plan ở `IN_PRODUCTION`, backend tự hoàn trả tồn kho.
- Khi complete plan, backend tạo lô thành phẩm mới và yêu cầu `expiryDate`.

---

Nếu cần, tôi có thể viết thêm bản FE-ready gồm:
- Postman collection riêng cho central kitchen,
- file mock JSON đầy đủ cho từng endpoint,
- mapping label tiếng Việt + màu badge cho status/timeline.
