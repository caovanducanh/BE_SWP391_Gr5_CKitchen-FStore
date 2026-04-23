# Central Kitchen Staff API Master Documentation (Full & 100% Accurate)

Tài liệu này là bản đặc tả kỹ thuật đầy đủ và chính xác nhất cho module Bếp Trung Tâm (Central Kitchen). Toàn bộ API đều được ánh chiếu trực tiếp từ mã nguồn hệ thống.

---

## 1. Thông tin Vận hành & Quy ước
- **Base URL**: `/api/central-kitchen`
- **Xác thực**: `Authorization: Bearer <JWT_TOKEN>` (Role: `CENTRAL_KITCHEN_STAFF`).
- **Response chuẩn**:
```json
{
  "statusCode": 200,
  "message": "Thông báo thành công",
  "data": { ... }
}
```

---

## 2. Module QUẢN LÝ ĐƠN HÀNG (Order Management)

Dành cho việc tiếp nhận và xử lý đơn đặt hàng từ các Franchise Store.

### 2.1 Lấy danh sách đơn hàng
- **Method**: `GET`
- **URL**: `/api/central-kitchen/orders`
- **Permission**: `ORDER_VIEW`
- **Query Params**:
  - `status`: Lọc theo trạng thái (`PENDING`, `ASSIGNED`, `IN_PROGRESS`, `PACKED_WAITING_SHIPPER`, `SHIPPING`, `DELIVERED`, `CANCELLED`).
  - `storeId`: Lọc theo ID cửa hàng.
  - `page` (Mặc định 0), `size` (Mặc định 20).

### 2.2 Xem chi tiết đơn hàng
- **Method**: `GET`
- **URL**: `/api/central-kitchen/orders/{orderId}`
- **Permission**: `ORDER_VIEW`

### 2.3 Tiếp nhận đơn hàng (Assign)
- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/orders/{orderId}/assign`
- **Permission**: `ORDER_ASSIGN`
- **Nghiệp vụ**: Hệ thống tự động gán đơn vào bếp của nhân viên đang đăng nhập và đổi trạng thái sang `ASSIGNED`.

### 2.4 Cập nhật trạng thái đơn hàng
- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/orders/{orderId}/status`
- **Permission**: `ORDER_STATUS_UPDATE`
- **Body**: `{"status": "...", "notes": "..."}`
- **Quan trọng**: Khi chuyển sang `PACKED_WAITING_SHIPPER`, hệ thống sẽ tự động trừ tồn kho các lô thành phẩm theo FEFO.

### 2.5 Lấy danh sách Trạng thái hợp lệ
- **Method**: `GET`
- **URL**: `/api/central-kitchen/order-statuses`
- **Permission**: `ORDER_STATUS_UPDATE`

---

## 3. Module SẢN XUẤT (Production Planning)

Quy trình từ lập kế hoạch đến khi bánh ra lò vào kho.

### 3.1 Kiểm tra khả năng sản xuất (Recipe Check)
- **Method**: `GET`
- **URL**: `/api/central-kitchen/products/{productId}/recipe-check`
- **Permission**: `PRODUCTION_PLAN_CREATE`
- **Query Params**: `quantity` (Số lượng muốn sản xuất).
- **Hành vi**: Kiểm tra xem kho nguyên liệu hiện tại có đủ để sản xuất số lượng này không.

### 3.2 Tạo kế hoạch sản xuất
- **Method**: `POST`
- **URL**: `/api/central-kitchen/production-plans`
- **Permission**: `PRODUCTION_PLAN_CREATE`
- **Body**: `{"productId": "...", "quantity": 100, "startDate": "...", "endDate": "...", "notes": "..."}`

### 3.3 Danh sách kế hoạch sản xuất
- **Method**: `GET`
- **URL**: `/api/central-kitchen/production-plans`
- **Permission**: `PRODUCTION_PLAN_VIEW`
- **Query Params**: `page`, `size`.

### 3.4 Bắt đầu sản xuất (Start)
- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/production-plans/{planId}/start`
- **Permission**: `PRODUCTION_PLAN_UPDATE`
- **Hành vi**: Trừ kho nguyên liệu thật sự tại các lô đã được chỉ định.

### 3.5 Hoàn thành sản xuất (Complete)
- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/production-plans/{planId}/complete`
- **Permission**: `PRODUCTION_PLAN_UPDATE`
- **Body**: `{"notes": "...", "expiryDate": "yyyy-MM-dd"}`
- **Hành vi**: Sinh ra Lô thành phẩm (`ProductBatch`) mới trong kho.

### 3.6 Hủy kế hoạch sản xuất (Cancel)
- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/production-plans/{planId}/cancel`
- **Permission**: `PRODUCTION_PLAN_UPDATE`
- **Hành vi**: Hoàn trả nguyên liệu về các lô cũ nếu kế hoạch đã Start.

---

## 4. Module QUẢN LÝ KHO THÀNH PHẨM (Product Inventory & Batches)

Dành cho nhân viên kiểm soát lượng bánh đã sản xuất xong.

### 4.1 Xem tồn kho thành phẩm (Tổng hợp)
- **Method**: `GET`
- **URL**: `/api/central-kitchen/inventory/products`
- **Permission**: `PRODUCTION_PLAN_VIEW`
- **Query Params**: `productId`, `productName`, `page`, `size`.
- **Dữ liệu trả về**: Tổng số lượng từng loại bánh hiện có tại bếp.

### 4.2 Danh sách các lô thành phẩm (Chi tiết lô)
- **Method**: `GET`
- **URL**: `/api/central-kitchen/product-batches`
- **Permission**: `PRODUCTION_PLAN_VIEW`
- **Query Params**: `productId`, `status` (`AVAILABLE`, `DEPLETED`), `page`, `size`.

### 4.3 Xem chi tiết một lô thành phẩm
- **Method**: `GET`
- **URL**: `/api/central-kitchen/product-batches/{batchId}`
- **Permission**: `PRODUCTION_PLAN_VIEW`
- **Hành vi**: Xem thông tin lô và danh sách các lô nguyên liệu (`ingredientBatchUsages`) cấu thành nên lô bánh này.

### 4.4 Cập nhật thông tin lô thành phẩm
- **Method**: `PATCH`
- **URL**: `/api/central-kitchen/product-batches/{batchId}`
- **Permission**: `PRODUCTION_PLAN_UPDATE`
- **Body**: `{"expiryDate": "...", "status": "...", "notes": "..."}`

---

## 5. Module QUẢN LÝ KHO NGUYÊN LIỆU (Ingredient Inventory & Batches)

Kiểm soát nguyên liệu đầu vào.

### 5.1 Xem tồn kho nguyên liệu (Tổng hợp)
- **Method**: `GET`
- **URL**: `/api/central-kitchen/inventory`
- **Permission**: `KITCHEN_INVENTORY_VIEW`
- **Query Params**: `ingredientId`, `ingredientName`, `lowStock` (boolean), `page`, `size`.

### 5.2 Nhập lô nguyên liệu mới
- **Method**: `POST`
- **URL**: `/api/central-kitchen/ingredient-batches`
- **Permission**: `KITCHEN_INVENTORY_CREATE`
- **Body**: `ImportIngredientBatchRequest` (Chi tiết: `ingredientId`, `batchNo`, `quantity`, `expiryDate`, `supplier`, `importPrice`).

### 5.3 Danh sách các lô nguyên liệu
- **Method**: `GET`
- **URL**: `/api/central-kitchen/ingredient-batches`
- **Permission**: `KITCHEN_INVENTORY_VIEW`
- **Query Params**: `ingredientId`, `ingredientName`, `status`, `page`, `size`.

### 5.4 Xem chi tiết một lô nguyên liệu
- **Method**: `GET`
- **URL**: `/api/central-kitchen/ingredient-batches/{id}`
- **Permission**: `KITCHEN_INVENTORY_VIEW`

---

## 6. Module TIỆN ÍCH & TỔNG QUAN (Utility & Overview)

### 6.1 Tổng quan vận hành bếp (Dashboard)
- **Method**: `GET`
- **URL**: `/api/central-kitchen/overview`
- **Permission**: `ORDER_VIEW`
- **Query Params**: `fromDate`, `toDate`.
- **Dữ liệu**: Số lượng đơn chờ, đơn đang làm, đơn quá hạn...

### 6.2 Thông tin Bếp của tôi
- **Method**: `GET`
- **URL**: `/api/central-kitchen/my-kitchen`
- **Permission**: `KITCHEN_INVENTORY_VIEW`

### 6.3 Tra cứu danh sách Sản phẩm (Để lập kế hoạch)
- **Method**: `GET`
- **URL**: `/api/central-kitchen/products`
- **Permission**: `PRODUCTION_PLAN_VIEW`
- **Query Params**: `search`, `category`, `page`, `size`.

### 6.4 Tra cứu danh sách Cửa hàng
- **Method**: `GET`
- **URL**: `/api/central-kitchen/stores`
- **Permission**: `STORE_VIEW`
- **Query Params**: `name`, `status`, `page`, `size`.

---

## 7. Checklist Tích hợp FE (Nghiệp vụ Quan trọng)
- [ ] **Lập kế hoạch**: Gọi `GET /products/{id}/recipe-check` để kiểm tra tồn kho ảo trước khi gọi `POST /production-plans`.
- [ ] **Sản xuất**: Nút "Bắt đầu" gọi API `/start`, nút "Hoàn tất" gọi API `/complete` (Bắt buộc truyền `expiryDate`).
- [ ] **Đóng gói đơn hàng**: Khi đổi status đơn sang `PACKED_WAITING_SHIPPER`, nếu Backend trả lỗi 400, hãy hiển thị thông báo "Thiếu tồn kho thành phẩm" để nhân viên biết cần đi sản xuất thêm.
- [ ] **Truy vết (Traceability)**: Trong màn hình chi tiết lô thành phẩm (`PB...`), luôn hiển thị danh sách `ingredientBatchUsages` để minh bạch nguồn gốc nguyên liệu.
