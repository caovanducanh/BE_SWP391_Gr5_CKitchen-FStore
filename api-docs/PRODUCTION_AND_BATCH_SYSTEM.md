# Production & Batch System - API Contract Chặt Chẽ (FE + QA)

Tài liệu này là bản contract chính thức cho luồng sản xuất và quản lý lô tại bếp trung tâm.
Mục tiêu là để Frontend đọc nhanh, mock đúng, test đúng và tích hợp API ổn định.

Phạm vi tài liệu:
- Ingredient batches (lô nguyên liệu)
- Kitchen inventory (tồn kho tổng hợp + chi tiết lô)
- Production plans (lập kế hoạch, start, complete, cancel)
- Product batches (lô thành phẩm)

---

## 1) Quy ước chung

### 1.1 Base URL và Auth
- Prefix: `/api/central-kitchen`
- Header bắt buộc:
  - `Authorization: Bearer <access_token>`
  - `Content-Type: application/json`

### 1.2 Success response wrapper
Các endpoint trong module này dùng `@ApiResponse`, nên response thành công có dạng:

```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

Status thường gặp:
- `GET` -> `200`
- `PATCH` -> `200`
- `POST` -> `201`

### 1.3 Error response wrapper

```json
{
  "statusCode": 400,
  "message": "...",
  "data": null
}
```

Validation lỗi body:

```json
{
  "statusCode": 400,
  "message": "Validation failed",
  "data": {
    "fieldName": "error message"
  }
}
```

### 1.4 Pagination
Các API list nhận `page` và `size`.

Frontend nên parse tối thiểu:
- `data.content` (mảng item)
- Metadata phân trang (`totalElements`, `totalPages`, `pageNumber`/`page.number`, `pageSize`/`page.size`, `last`)

---

## 2) Bản đồ endpoint chuẩn

## 2.1 Ingredient Batches
- `POST /api/central-kitchen/ingredient-batches`
- `GET /api/central-kitchen/ingredient-batches`
- `GET /api/central-kitchen/ingredient-batches/{id}`

## 2.2 Inventory
- `GET /api/central-kitchen/inventory`

## 2.3 Production Plans
- `GET /api/central-kitchen/production-plans`
- `POST /api/central-kitchen/production-plans`
- `GET /api/central-kitchen/production-plans/{planId}`
- `PATCH /api/central-kitchen/production-plans/{planId}/start`
- `PATCH /api/central-kitchen/production-plans/{planId}/complete`
- `PATCH /api/central-kitchen/production-plans/{planId}/cancel`

## 2.4 Product Batches
- `GET /api/central-kitchen/product-batches`
- `GET /api/central-kitchen/product-batches/{batchId}`

---

## 3) Trạng thái và state machine

## 3.1 ProductionPlan.status
- `DRAFT`
- `APPROVED`
- `IN_PRODUCTION`
- `COMPLETED`
- `CANCELLED`

Luồng thực tế:
- `DRAFT` hoặc `APPROVED` -> `IN_PRODUCTION`
- `IN_PRODUCTION` -> `COMPLETED`
- `DRAFT`/`APPROVED`/`IN_PRODUCTION` -> `CANCELLED`
- `COMPLETED` không được cancel

## 3.2 IngredientBatch.status
- `ACTIVE`
- `DEPLETED`
- `EXPIRED` (được support ở model, chưa có job auto trong module này)
- `DISPOSED` (được support ở model)

## 3.3 Product Batch status
- `AVAILABLE` (status được set khi complete plan)
- Model có hỗ trợ thêm: `PARTIALLY_DISTRIBUTED`, `FULLY_DISTRIBUTED`

---

## 4) Quy tắc nghiệp vụ cốt lõi (FEFO)

## 4.1 Tạo plan (reserve theo FEFO, chưa trừ kho)
Khi tạo plan:
1. Kiểm tra user có kitchen.
2. Kiểm tra `endDate > startDate`.
3. Kiểm tra product tồn tại và có recipe.
4. Với từng nguyên liệu trong recipe:
   - Tính `requiredTotal = recipe.quantity * plan.quantity`.
   - Quét lô `ACTIVE` theo `expiryDate` tăng dần.
   - Reserve từ lô sớm hết hạn trước (FEFO).
5. Nếu thiếu nguyên liệu ở bất kỳ ingredient nào -> trả lỗi ngay.
6. Lưu plan ở trạng thái `DRAFT`.

Lưu ý:
- Bước này tạo record usage (reserve) nhưng chưa trừ `remainingQuantity` của lô và chưa trừ `KitchenInventory.totalQuantity`.

## 4.2 Start plan (trừ kho thật)
Khi start:
1. Chỉ cho phép plan ở `DRAFT` hoặc `APPROVED`.
2. Duyệt toàn bộ usage đã reserve.
3. Trừ `remainingQuantity` từng lô.
4. Nếu lô về 0 -> set `DEPLETED`.
5. Trừ `KitchenInventory.totalQuantity` tương ứng.
6. Chuyển plan sang `IN_PRODUCTION`.

Nếu một lô không còn đủ lượng tại thời điểm start:
- API trả lỗi và yêu cầu recreate plan.

## 4.3 Complete plan (tạo lô thành phẩm)
Khi complete:
1. Chỉ cho phép plan ở `IN_PRODUCTION`.
2. `expiryDate` của thành phẩm là bắt buộc.
3. Chuyển plan sang `COMPLETED`.
4. Tạo mới `Batch` (lô thành phẩm):
   - `id`: dạng `PByyMMddHHmmss`
   - `quantity = plan.quantity`
   - `remainingQuantity = plan.quantity`
   - `status = AVAILABLE`

## 4.4 Cancel plan (rollback khi đang sản xuất)
Khi cancel:
- Nếu plan là `COMPLETED` -> từ chối.
- Nếu plan đang `IN_PRODUCTION`:
  - Hoàn lại số lượng vào đúng các lô đã dùng.
  - Nếu lô đang `DEPLETED` và hoàn > 0 -> chuyển lại `ACTIVE`.
  - Cộng lại `KitchenInventory.totalQuantity`.
  - Xóa usage records của plan.
- Cuối cùng set plan `CANCELLED`.

---

## 5) Contract chi tiết từng API

## 5.1 POST /api/central-kitchen/ingredient-batches
Permission: `KITCHEN_INVENTORY_CREATE`

Mục đích: Nhập một lô nguyên liệu mới vào bếp.

Request body:

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

Validate quan trọng:
- `ingredientId`: bắt buộc
- `batchNo`: bắt buộc, max 30
- `quantity`: bắt buộc, > 0
- `expiryDate`: bắt buộc, không được < ngày hiện tại
- `supplier`: max 100
- `notes`: max 500
- `minStock`: >= 0

Rule quan trọng:
- `batchNo` phải unique theo cặp `(kitchenId, ingredientId)`.
- `unit` của lô lấy từ `Ingredient.unit`, không lấy từ request.
- Nếu `importDate` null -> mặc định ngày hiện tại.
- Set `status = ACTIVE`.
- Đồng thời upsert `KitchenInventory.totalQuantity`.

Lỗi thường gặp:
- `400 Số lô '...' đã tồn tại cho nguyên liệu này trong bếp`
- `400 Ngày hết hạn không được nhỏ hơn ngày hiện tại`
- `404 Không tìm thấy nguyên liệu: ...`

## 5.2 GET /api/central-kitchen/ingredient-batches
Permission: `KITCHEN_INVENTORY_VIEW`

Query params:
- `ingredientId` (optional)
- `ingredientName` (optional, contains + ignore case)
- `status` (optional, uppercase compare)
- `page` (default `0`)
- `size` (default `20`)

Sort mặc định: `expiryDate ASC`.

Response item (`IngredientBatchResponse`) gồm:
- `id`, `kitchenId`, `kitchenName`
- `ingredientId`, `ingredientName`
- `batchNo`
- `initialQuantity`, `remainingQuantity`, `unit`
- `expiryDate`, `supplier`, `importPrice`, `importDate`
- `status`, `notes`, `nearExpiry`, `createdAt`, `updatedAt`

`nearExpiry = true` khi `expiryDate <= hiện tại + 30 ngày`.

## 5.3 GET /api/central-kitchen/ingredient-batches/{id}
Permission: `KITCHEN_INVENTORY_VIEW`

Mục đích: Lấy chi tiết 1 lô nguyên liệu.

Lỗi thường gặp:
- `404 Không tìm thấy lô nguyên liệu: {id}`
- `400 Lô này không thuộc bếp của bạn`

## 5.4 GET /api/central-kitchen/inventory
Permission: `KITCHEN_INVENTORY_VIEW`

Query params:
- `ingredientId` (optional)
- `ingredientName` (optional)
- `lowStock` (optional)
- `page` (default `0`)
- `size` (default `20`)

Rule filter low stock:
- `lowStock=true` -> `totalQuantity <= minStock`
- `lowStock=false` -> `totalQuantity > minStock`

Response item (`KitchenInventoryDetailResponse`) gồm:
- `id`, `kitchenId`, `kitchenName`
- `ingredientId`, `ingredientName`
- `totalQuantity`, `unit`, `minStock`, `lowStock`
- `batches`: danh sách lô `ACTIVE` của ingredient (sort theo FEFO)

## 5.5 GET /api/central-kitchen/production-plans
Permission: `PRODUCTION_PLAN_VIEW`

Query params:
- `page` (default `0`)
- `size` (default `20`)

Sort mặc định: `createdAt DESC`.

Lưu ý hiện tại:
- API list đang lấy toàn bộ plan trong hệ thống theo phân trang.
- API chi tiết theo id vẫn có kiểm tra kế hoạch thuộc kitchen của user.

## 5.6 POST /api/central-kitchen/production-plans
Permission: `PRODUCTION_PLAN_CREATE`

Request body:

```json
{
  "productId": "PROD001",
  "quantity": 100,
  "startDate": "2026-04-23T08:00:00",
  "endDate": "2026-04-23T17:00:00",
  "notes": "Sản xuất ca sáng"
}
```

Validate:
- `productId`: bắt buộc
- `quantity`: bắt buộc, >= 1
- `startDate`: bắt buộc
- `endDate`: bắt buộc và phải sau `startDate`
- `notes`: max 500

Response (`ProductionPlanResponse`) gồm:
- `id`, `productId`, `productName`
- `kitchenId`, `kitchenName`
- `quantity`, `unit`, `status`
- `startDate`, `endDate`, `staff`, `notes`
- `createdAt`, `updatedAt`
- `ingredients` (khi có):
  - `ingredientId`, `ingredientName`
  - `requiredQuantity`, `availableQuantity`, `unit`, `sufficient`

Lỗi thường gặp:
- `400 You are not assigned to any kitchen`
- `400 endDate must be after startDate`
- `404 Product not found: ...`
- `400 Product has no recipe defined`
- `400 Not enough ingredient: ... Short by: ...`

## 5.7 GET /api/central-kitchen/production-plans/{planId}
Permission: `PRODUCTION_PLAN_VIEW`

Lỗi thường gặp:
- `404 Production plan not found: {planId}`
- `400 Plan does not belong to your kitchen`

## 5.8 PATCH /api/central-kitchen/production-plans/{planId}/start
Permission: `PRODUCTION_PLAN_UPDATE`

Body: không có.

Rule:
- Chỉ start khi plan là `DRAFT` hoặc `APPROVED`.
- Nếu batch thay đổi tồn kho làm thiếu lượng reserve -> trả lỗi.

Lỗi thường gặp:
- `404 Plan not found: {planId}`
- `400 Can only start DRAFT or APPROVED plans`
- `400 Batch <batchNo> does not have enough qty now. Please recreate plan.`

## 5.9 PATCH /api/central-kitchen/production-plans/{planId}/complete
Permission: `PRODUCTION_PLAN_UPDATE`

Request body:

```json
{
  "notes": "Hoàn tất ca sản xuất",
  "expiryDate": "2026-04-30"
}
```

Rule:
- Plan phải đang `IN_PRODUCTION`.
- `expiryDate` bắt buộc.
- Backend tạo lô thành phẩm mới (`Batch`) status `AVAILABLE`.

Lỗi thường gặp:
- `404 Plan not found: {planId}`
- `400 Plan is not in production`
- `400 expiryDate is required for the finished product batch`

## 5.10 PATCH /api/central-kitchen/production-plans/{planId}/cancel
Permission: `PRODUCTION_PLAN_UPDATE`

Request body:

```json
{
  "notes": "Hủy do thay đổi kế hoạch"
}
```

Rule:
- Không cho hủy nếu đã `COMPLETED`.
- Nếu plan đang `IN_PRODUCTION` thì backend tự rollback kho.

Lỗi thường gặp:
- `404 Plan not found: {planId}`
- `400 Cannot cancel completed plan`

## 5.11 GET /api/central-kitchen/product-batches
Permission: `PRODUCTION_PLAN_VIEW`

Query params:
- `productId` (optional)
- `status` (optional)
- `page` (default `0`)
- `size` (default `20`)

Rule:
- Tự động filter theo kitchen của user đăng nhập.

Response item (`BatchResponse`) gồm:
- `id`, `planId`, `productId`, `productName`
- `kitchenId`, `kitchenName`
- `quantity`, `remainingQuantity`, `unit`
- `expiryDate`, `status`, `staff`, `notes`
- `createdAt`, `updatedAt`
- `ingredientBatchUsages` (traceability):
  - `ingredientBatchId`, `ingredientId`, `ingredientName`
  - `batchNo`, `quantityUsed`, `unit`, `expiryDate`

## 5.12 GET /api/central-kitchen/product-batches/{batchId}
Permission: `PRODUCTION_PLAN_VIEW`

Lỗi thường gặp:
- `404 Batch not found: {batchId}`
- `400 Batch does not belong to your kitchen`

---

## 6) Mẫu flow tích hợp FE đề xuất

1. Nhập lô nguyên liệu:
   - `POST /ingredient-batches`
2. Kiểm tra tồn kho tổng hợp:
   - `GET /inventory`
3. Tạo kế hoạch sản xuất:
   - `POST /production-plans`
4. Xem chi tiết kế hoạch (kèm ingredient required/available):
   - `GET /production-plans/{planId}`
5. Bắt đầu sản xuất (trừ kho thật):
   - `PATCH /production-plans/{planId}/start`
6. Hoàn thành sản xuất (tạo lô thành phẩm):
   - `PATCH /production-plans/{planId}/complete`
7. Theo dõi lô thành phẩm:
   - `GET /product-batches`
   - `GET /product-batches/{batchId}`

Flow hủy:
- Nếu plan đang sản xuất dở: gọi `PATCH /production-plans/{planId}/cancel` để rollback kho.

---

## 7) Test matrix cho FE/QA

## 7.1 Happy path
- Import batch thành công -> kiểm tra `status=ACTIVE`, `remainingQuantity=initialQuantity`.
- Tạo plan với nhiều lô cùng ingredient -> verify FEFO reserve đúng thứ tự hạn.
- Start plan -> verify quantity từng lô bị trừ đúng.
- Start plan làm lô về 0 -> verify lô chuyển `DEPLETED`.
- Complete plan -> verify tạo product batch mới với `status=AVAILABLE`.
- Cancel plan đang `IN_PRODUCTION` -> verify kho được hoàn lại đúng.

## 7.2 Fail path
- Tạo plan khi thiếu nguyên liệu -> nhận lỗi `Not enough ingredient...`.
- Start plan không ở `DRAFT/APPROVED` -> nhận lỗi transition.
- Start plan khi stock đã đổi sau lúc reserve -> nhận lỗi thiếu qty hiện tại.
- Complete plan khi chưa start -> nhận lỗi `Plan is not in production`.
- Complete plan thiếu `expiryDate` -> nhận lỗi validate nghiệp vụ.
- Cancel plan đã `COMPLETED` -> nhận lỗi không cho hủy.
- Import batch trùng `batchNo` trong cùng kitchen+ingredient -> bị chặn.

## 7.3 Regression checklist
- `KitchenInventory.totalQuantity` luôn khớp tổng `remainingQuantity` từ các lô active sau start/cancel.
- `ingredientBatchUsages` của product batch luôn truy vết được lô nguyên liệu đã dùng.
- `nearExpiry` bật đúng khi hạn <= 30 ngày.

---

## 8) Khác biệt quan trọng so với tài liệu cũ

1. Ingredient batch API đúng là:
   - `/api/central-kitchen/ingredient-batches`
   - Không dùng `/api/ingredient-batches` hay `/kitchen/my`.
2. Start/Complete/Cancel dùng `PATCH`, không dùng `POST`.
3. Create plan mặc định `status=DRAFT`.
4. Product batch status được tạo mặc định là `AVAILABLE`.
5. Cancel khi đang `IN_PRODUCTION` có rollback kho tự động.

---

## 9) Ghi chú triển khai FE

- Không hardcode state transition ngoài rule ở mục 3.
- Luôn hiển thị message lỗi từ backend để dễ debug nghiệp vụ kho.
- Với list endpoint, ưu tiên normalize pagination ở tầng API client để dùng thống nhất trong UI.
- Với các trường datetime (`LocalDateTime`), backend không kèm timezone; FE nên thống nhất hiển thị theo múi giờ hệ thống.
