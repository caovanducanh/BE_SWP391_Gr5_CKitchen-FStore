# Sales & Revenue API Integration Guide (Store Staff + Store Manager)

Tài liệu này dành cho Frontend tích hợp đầy đủ nhóm API doanh số bán hàng và doanh thu.

Phạm vi tài liệu:
- Store Staff: upload/import doanh số theo ngày, clear báo cáo, xem lịch sử doanh số của chính cửa hàng.
- Store Manager: xem doanh thu theo ngày, tổng doanh thu theo cửa hàng, lấy danh sách store/kitchen để filter, export Excel doanh thu.

Mục tiêu:
- Nối API đúng payload/request format.
- Hiểu rõ validate và lỗi nghiệp vụ để xử lý UX chuẩn.
- Tránh sai lệch số liệu do quy ước ngày, filter và import lại.

---

## 1. Base URL, Auth, Response Wrapper

### 1.1 Base URL
- Local: http://localhost:8080
- Production: theo biến môi trường FE (`VITE_API_BASE_URL`)

### 1.2 Authentication
Tất cả API trong tài liệu này cần JWT Bearer token.

Header mẫu:
```http
Authorization: Bearer <access_token>
```

### 1.3 Chuẩn response thành công
Hầu hết endpoint trả về dạng:
```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

### 1.4 Chuẩn response lỗi
```json
{
  "statusCode": 400,
  "message": "...",
  "data": null
}
```

Tham chiếu thêm: `api-docs/error-response.md`.

---

## 2. Permission Matrix

### 2.1 Store Staff
- `SALES_REPORT_TEMPLATE_DOWNLOAD`: tải file template Excel
- `SALES_REPORT_IMPORT`: import báo cáo doanh số
- `SALES_REPORT_CLEAR`: xóa báo cáo đã import (để import lại)
- `SALES_REPORT_VIEW_OWN`: xem doanh số của chính store staff

### 2.2 Store Manager
- `SALES_REPORT_VIEW`: xem báo cáo doanh thu manager, export, danh sách filter

Lưu ý:
- Endpoint được kiểm soát bởi `@SecuredEndpoint`, thiếu quyền sẽ trả 403.

---

## 3. Tổng quan Endpoint

## 3.1 Store Staff APIs (prefix `/api/store/sales`)
- `GET /template`: download Excel template
- `POST /import`: import file Excel doanh số theo ngày
- `DELETE ?date=yyyy-MM-dd`: clear báo cáo doanh số ngày cụ thể
- `GET /daily`: danh sách báo cáo doanh số theo ngày (có phân trang)
- `GET /daily/detail`: chi tiết item bán theo một ngày (phân trang item)

## 3.2 Manager APIs (prefix `/api/manager/sales`)
- `GET /daily`: doanh thu theo từng ngày, tách theo store
- `GET /total`: tổng doanh thu trong khoảng ngày, có breakdown theo store
- `GET /stores`: lấy toàn bộ cửa hàng cho filter
- `GET /kitchens`: lấy toàn bộ bếp cho filter UI
- `GET /daily/export`: xuất Excel báo cáo doanh thu theo ngày

---

## 4. Store Staff - API Chi Tiết

## 4.1 Download Sales Template

- Method: `GET`
- URL: `/api/store/sales/template`
- Permission: `SALES_REPORT_TEMPLATE_DOWNLOAD`
- Response: file nhị phân `.xlsx`

### Content-Disposition
```http
attachment; filename=sales_report_template.xlsx
```

### Cấu trúc sheet template
Sheet `sales_report` có 4 cột:
1. `product_id`
2. `quantity`
3. `unit`
4. `unit_price`

Sheet `notes` mô tả rule.

### Rule quan trọng
- Không truyền `store_id` trong Excel.
- Không truyền `sale_date` trong Excel.
- Ngày báo cáo lấy từ query param `date` khi gọi API import.
- Store lấy từ account store staff đang đăng nhập.

---

## 4.2 Import Sales Report

- Method: `POST`
- URL: `/api/store/sales/import`
- Permission: `SALES_REPORT_IMPORT`
- Content-Type: `multipart/form-data`
- Form fields:
  - `file`: file `.xlsx`
  - `date`: định dạng `yyyy-MM-dd`

### cURL mẫu
```bash
curl -X POST "http://localhost:8080/api/store/sales/import?date=2026-04-20" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@sales_report.xlsx"
```

### Response thành công
```json
{
  "statusCode": 200,
  "message": "Sales report imported successfully",
  "data": {
    "reportId": "SR0123",
    "storeId": "ST001",
    "reportDate": "2026-04-20",
    "itemCount": 10,
    "totalQuantity": 78,
    "totalRevenue": 2150000,
    "importedBy": "store_staff_01",
    "importedAt": "2026-04-20T22:13:30"
  }
}
```

### Validate & nghiệp vụ anti-fraud
1. File bắt buộc, không rỗng.
2. `date` bắt buộc.
3. Không cho import trùng cùng store + cùng date (phải clear trước).
4. Mỗi dòng Excel:
   - `product_id` bắt buộc, phải tồn tại trong catalog.
   - `quantity` phải là số nguyên dương.
   - `unit` bắt buộc.
   - `unit_price` phải >= 0.
5. Tất cả product trong Excel phải có trong inventory của store hiện tại.
6. Tổng quantity bán theo từng sản phẩm không được vượt quá tồn kho hiện tại.
7. `unit` trong Excel phải trùng với unit trong inventory sản phẩm.

### Tác động dữ liệu khi import thành công
- Tạo 1 record trong `sales_records`.
- Tạo nhiều record trong `sale_items`.
- Trừ tồn kho `store_inventory` theo tổng lượng bán từng sản phẩm.

### Lỗi thường gặp
- 400 `Excel file is required`
- 400 `Report date is required`
- 400 `No valid sale item rows found in Excel file`
- 400 `Unknown product_id: ...`
- 400 `Store inventory not found for product_id: ...`
- 400 `Invalid sales report data: product_id ... sold ... but stock is only ...`
- 400 `Invalid Excel format. Please upload a valid .xlsx file`
- 409 `Sales report for this date already exists. Please clear it before re-importing.`

---

## 4.3 Clear Sales Report By Date

- Method: `DELETE`
- URL: `/api/store/sales?date=yyyy-MM-dd`
- Permission: `SALES_REPORT_CLEAR`

### Response thành công
```json
{
  "statusCode": 200,
  "message": "Sales report cleared successfully",
  "data": {
    "reportId": "SR0123",
    "storeId": "ST001",
    "reportDate": "2026-04-20",
    "restoredItems": 10,
    "restoredQuantity": 78,
    "clearedAt": "2026-04-20T23:05:00"
  }
}
```

### Nghiệp vụ khi clear
- Xóa toàn bộ `sale_items` của report đó.
- Xóa `sales_records` của ngày đó.
- Hoàn lại số lượng đã bán vào `store_inventory`.

Nếu inventory của product chưa tồn tại ở store (case dữ liệu thiếu), backend có thể tạo mới inventory record để restore số lượng.

### Lỗi thường gặp
- 400 `Report date is required`
- 404 `No sales report found for this date`

---

## 4.4 Get My Daily Sales (Paginated)

- Method: `GET`
- URL: `/api/store/sales/daily`
- Permission: `SALES_REPORT_VIEW_OWN`
- Query:
  - `fromDate` (optional, `yyyy-MM-dd`)
  - `toDate` (optional, `yyyy-MM-dd`)
  - `page` (default 0)
  - `size` (default 20)

### Rule filter
- Nếu truyền cả `fromDate` và `toDate` thì `fromDate <= toDate`.
- Sắp xếp mặc định: ngày giảm dần (`date DESC`).

### Response mẫu
```json
{
  "statusCode": 200,
  "message": "Daily sales reports retrieved successfully",
  "data": {
    "content": [
      {
        "reportId": "SR0123",
        "reportDate": "2026-04-20",
        "totalRevenue": 2150000,
        "itemCount": 10,
        "totalQuantity": 78,
        "recordedBy": "store_staff_01",
        "recordedAt": "2026-04-20T22:13:30"
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

### Lỗi thường gặp
- 400 `fromDate must be before or equal to toDate`
- 400 `page must be >= 0`
- 400 `size must be > 0`

---

## 4.5 Get My Daily Sales Detail (Paginated Items)

- Method: `GET`
- URL: `/api/store/sales/daily/detail`
- Permission: `SALES_REPORT_VIEW_OWN`
- Query:
  - `date` (required, `yyyy-MM-dd`)
  - `page` (default 0)
  - `size` (default 20)

### Response mẫu
```json
{
  "statusCode": 200,
  "message": "Daily sales detail retrieved successfully",
  "data": {
    "reportId": "SR0123",
    "storeId": "ST001",
    "storeName": "Store District 1",
    "reportDate": "2026-04-20",
    "totalRevenue": 2150000,
    "itemCount": 10,
    "totalQuantity": 78,
    "recordedBy": "store_staff_01",
    "recordedAt": "2026-04-20T22:13:30",
    "page": 0,
    "size": 20,
    "totalItems": 10,
    "totalPages": 1,
    "hasNext": false,
    "items": [
      {
        "productId": "PROD001",
        "productName": "Croissant",
        "quantity": 5,
        "unit": "pcs",
        "unitPrice": 25000,
        "lineTotal": 125000
      }
    ]
  }
}
```

### Lỗi thường gặp
- 400 `Report date is required`
- 400 `page must be >= 0`
- 400 `size must be > 0`
- 404 `No sales report found for this date`

---

## 5. Store Manager - API Chi Tiết

## 5.1 Get Daily Revenue By Date Range

- Method: `GET`
- URL: `/api/manager/sales/daily`
- Permission: `SALES_REPORT_VIEW`
- Query:
  - `fromDate` (optional)
  - `toDate` (optional)
  - `storeId` (optional)

Nếu truyền `storeId` thì dữ liệu trong `days[].stores` chỉ còn store đó.

### Response mẫu
```json
{
  "statusCode": 200,
  "message": "Daily revenue report retrieved successfully",
  "data": {
    "fromDate": "2026-04-01",
    "toDate": "2026-04-30",
    "totalRevenue": 25000,
    "dayCount": 1,
    "days": [
      {
        "reportDate": "2026-04-20",
        "totalRevenue": 25000,
        "storeCount": 1,
        "stores": [
          {
            "storeId": "ST001",
            "storeName": "Store District 1",
            "totalRevenue": 25000,
            "reportCount": 1
          }
        ]
      }
    ]
  }
}
```

### Lỗi thường gặp
- 400 `fromDate must be before or equal to toDate`

---

## 5.2 Get Total Revenue (Store Breakdown)

- Method: `GET`
- URL: `/api/manager/sales/total`
- Permission: `SALES_REPORT_VIEW`
- Query:
  - `fromDate` (optional)
  - `toDate` (optional)
  - `storeId` (optional)

### Hành vi quan trọng
- Không truyền `storeId`:
  - Trả tổng doanh thu toàn hệ theo range.
  - Trả breakdown theo từng store trong `stores`.
- Có truyền `storeId`:
  - Vẫn trả `stores` dạng list, nhưng list chỉ có 1 store.
  - Phù hợp FE giữ một model dữ liệu thống nhất.

### Response mẫu (không truyền storeId)
```json
{
  "statusCode": 200,
  "message": "Store total revenue retrieved successfully",
  "data": {
    "fromDate": "2026-04-01",
    "toDate": "2026-04-30",
    "totalReportRevenue": 6025000,
    "storeCount": 2,
    "stores": [
      {
        "storeId": "ST001",
        "storeName": "Store District 1",
        "totalReportRevenue": 25000
      },
      {
        "storeId": "ST002",
        "storeName": "Store District 2",
        "totalReportRevenue": 6000000
      }
    ]
  }
}
```

### Response mẫu (có storeId=ST001)
```json
{
  "statusCode": 200,
  "message": "Store total revenue retrieved successfully",
  "data": {
    "fromDate": "2026-04-01",
    "toDate": "2026-04-30",
    "totalReportRevenue": 25000,
    "storeCount": 1,
    "stores": [
      {
        "storeId": "ST001",
        "storeName": "Store District 1",
        "totalReportRevenue": 25000
      }
    ]
  }
}
```

### Lỗi thường gặp
- 400 `fromDate must be before or equal to toDate`

---

## 5.3 Get All Stores For Filter

- Method: `GET`
- URL: `/api/manager/sales/stores`
- Permission: `SALES_REPORT_VIEW`

### Response
`data` là array `StoreResponse` (id, name, address, phone, manager, status, openDate, createdAt, updatedAt).

Mục đích FE:
- Populate dropdown store filter cho màn hình doanh thu.

---

## 5.4 Get All Kitchens For Filter

- Method: `GET`
- URL: `/api/manager/sales/kitchens`
- Permission: `SALES_REPORT_VIEW`

### Response
`data` là array `KitchenResponse` (id, name, address, phone, capacity, status, createdAt, updatedAt).

Mục đích FE:
- Đồng bộ filter UI (nếu màn hình doanh thu dùng bộ lọc chung với màn hình khác).

---

## 5.5 Export Daily Revenue Report (Excel)

- Method: `GET`
- URL: `/api/manager/sales/daily/export`
- Permission: `SALES_REPORT_VIEW`
- Query:
  - `fromDate` (optional)
  - `toDate` (optional)
  - `storeId` (optional)

### Response
- Content-Type:
  - `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- File name:
  - `manager_daily_revenue_yyyyMMdd_HHmmss.xlsx`

### Workbook layout
- Sheet `summary`:
  - from_date, to_date, store_filter, day_count, total_revenue
  - bảng ngang `Store Revenue Breakdown` (mỗi cột là 1 store)
- Sheet `daily_details`:
  - `report_date`, `store_id`, `store_name`, `store_total_revenue`

### Lỗi thường gặp
- 400 `fromDate must be before or equal to toDate`
- 500 `Cannot export manager revenue report`

---

## 6. Frontend Integration Flow Khuyến Nghị

## 6.1 Store Staff (cuối ngày)
1. Tải template từ `/api/store/sales/template` (nếu cần).
2. Người dùng điền Excel đúng cột/rule.
3. Gọi `/api/store/sales/import?date=...`.
4. Nếu cần sửa dữ liệu:
   - Gọi `/api/store/sales?date=...` để clear.
   - Import lại file đã sửa.
5. Hiển thị lịch sử từ `/api/store/sales/daily` và chi tiết từ `/api/store/sales/daily/detail`.

## 6.2 Store Manager (theo dõi)
1. Load dropdown store bằng `/api/manager/sales/stores`.
2. Khi user chọn range/store:
   - Gọi `/api/manager/sales/total` để lấy KPI tổng + breakdown store.
   - Gọi `/api/manager/sales/daily` để render chart theo ngày.
3. Khi bấm export, gọi `/api/manager/sales/daily/export`.

---

## 7. Edge Cases FE Cần Xử Lý

1. Dữ liệu rỗng:
- `days: []` hoặc `stores: []` là bình thường khi chưa có report trong range.

2. Không truyền from/to date:
- Backend cho phép null, FE có thể dùng default range theo UX.

3. Định dạng ngày:
- Luôn gửi ISO date `yyyy-MM-dd`.

4. Đồng bộ timezone:
- Backend xử lý theo Asia/Ho_Chi_Minh cho mốc ghi nhận.
- FE nên hiển thị date/time nhất quán timezone VN.

5. Idempotency import:
- Cùng store + cùng date chỉ có 1 report.
- Muốn import lại bắt buộc clear trước.

6. Token/permission lỗi:
- 401 khi token sai/hết hạn.
- 403 khi user thiếu quyền tương ứng.

---

## 8. Quick Test Checklist (QA/UAT)

### Store Staff
- [ ] Download template thành công.
- [ ] Import file hợp lệ thành công.
- [ ] Import trùng ngày báo 409.
- [ ] Import quantity vượt stock báo 400.
- [ ] Clear report thành công và có thể import lại cùng ngày.
- [ ] Daily list phân trang đúng.
- [ ] Daily detail phân trang item đúng.

### Manager
- [ ] `/daily` trả đúng `days[].stores` theo range.
- [ ] `/total` không truyền storeId trả breakdown nhiều store.
- [ ] `/total` có storeId vẫn trả list `stores` gồm đúng 1 phần tử.
- [ ] `/stores` và `/kitchens` trả danh sách đầy đủ.
- [ ] Export Excel mở được và đúng số liệu.

---

## 9. Ghi chú cho team

- API `GET /api/manager/sales/total` hiện đã bỏ field top-level `storeId` và `storeName` trong `data`.
- FE nên lấy thông tin store hoàn toàn từ `data.stores[]` để tránh null handling không cần thiết.
- Nếu cần mở rộng filter theo kitchen thật sự ở doanh thu, hiện tại API doanh thu chưa nhận `kitchenId`; cần bổ sung backend riêng.
