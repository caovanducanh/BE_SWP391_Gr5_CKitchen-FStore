# Hướng dẫn tích hợp API Luồng giao nhận hàng qua mã QR cho Shipper (FE)

Tài liệu này mô tả đầy đủ luồng giao nhận hàng mới:
- Điều phối viên sinh mã QR nhận đơn theo từng đơn hàng (Order).
- Mã QR được dán lên thùng hàng để Shipper quét nhận đơn.
- Hệ thống tự động sinh mã vận đơn (deliveryId) nếu chưa có.
- Shipper đánh dấu đã giao hàng thành công (chờ cửa hàng xác nhận).
- Nhân viên cửa hàng (Store Staff) xác nhận đã nhận đơn để hoàn tất quy trình.

**Mục tiêu**: Đội ngũ Frontend (FE) kết nối API theo đúng thứ tự, dễ hiểu và không cần phải đoán nghiệp vụ.

---

## 1) Tổng quan và Phạm vi

- **Prefix Điều phối viên (Supply Coordinator)**: `/api/supply-coordinator`
- **Prefix Shipper**: `/api/shipper`
- **Prefix Nhân viên cửa hàng (Store Staff)**: `/api/store`
- **Xác thực (Auth)**: JWT Bearer token
- **Content-Type**: `application/json`

**Phản hồi thành công (Success Response):**

```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

**Phản hồi lỗi (Error Response):**

```json
{
  "statusCode": 400,
  "message": "...",
  "data": null
}
```

---

## 2) Các trạng thái chính cần ánh xạ (Map) ở FE

### 2.1) Trạng thái Vận đơn (Delivery status)
- `ASSIGNED`: Đã được tạo, đang chờ shipper nhận.
- `SHIPPING`: Shipper đang đi giao hàng.
- `DELAYED`: Gặp sự cố (tùy chọn).
- `WAITING_CONFIRM`: Shipper đã giao tới nơi, chờ cửa hàng xác nhận.
- `DELIVERED`: Hoàn tất giao nhận.
- `CANCELLED`: Đã hủy.

### 2.2) Trạng thái Đơn hàng (Order status) liên quan
- `PACKED_WAITING_SHIPPER`: Đã đóng gói, chờ shipper.
- `SHIPPING`: Đang trong quá trình vận chuyển.
- `DELIVERED`: Đã giao hàng xong.
- `CANCELLED`: Đã hủy đơn.

### 2.3) Ý nghĩa các mốc thời gian
- `pickedUpAt`: Lúc shipper nhận đơn sau khi quét mã QR thành công.
- `deliveredAt`: Lúc shipper báo đã giao hàng thành công (qua nút mark-success).

---

## 3) Quy trình tích hợp chi tiết cho FE

### Bước 1: Điều phối viên sinh mã QR theo đơn hàng

**Endpoint:**
- **Method**: `GET`
- **URL**: `/api/supply-coordinator/orders/{orderId}/pickup-qr`
- **Quyền (Permission)**: `SUPPLY_DELIVERY_VIEW`

**Mục đích:**
- Lấy mã QR để dán lên thùng hàng.
- Nếu đơn hàng chưa có bản ghi giao hàng (delivery), backend sẽ tự động tạo mới.

**Dữ liệu phản hồi:**
```json
{
  "orderId": "ORD002",
  "deliveryId": "DEL0421008",
  "pickupQrCode": "PK-ORD002-3D451097",
  "deliveryStatus": "ASSIGNED"
}
```

---

### Bước 2: (Tùy chọn) Shipper xem các đơn đang chờ nhận

**Endpoint:**
- **Method**: `GET`
- **URL**: `/api/shipper/orders/available?page=0&size=20&lat=10.77&lon=106.7`
- **Quyền**: `SHIPPER_DELIVERY_VIEW`

**Mục đích:**
- Hiển thị danh sách các đơn đã đóng gói, có thông tin giao hàng nhưng chưa có người nhận.
- **Tính toán Khoảng cách**: Nếu truyền `lat` và `lon`, hệ thống sẽ tính khoảng cách (km) từ vị trí của shipper đến kho (Central Kitchen) và tự động sắp xếp đơn gần nhất lên đầu.

---

### Bước 3: Shipper quét mã QR để nhận đơn

**Endpoint:**
- **Method**: `POST`
- **URL**: `/api/shipper/deliveries/scan-qr`
- **Quyền**: `SHIPPER_DELIVERY_CLAIM`
- **Body**:
```json
{
  "qrCode": "PK-ORD002-3D451097"
}
```

---

### Bước 3.5: Shipper xem danh sách đơn mình đang nhận (My Deliveries)

**Endpoint:**
- **Method**: `GET`
- **URL**: `/api/shipper/deliveries/my?page=0&size=20&lat=10.77&lon=106.7`
- **Quyền**: `SHIPPER_DELIVERY_VIEW`

**Mục đích:**
- Xem lại các đơn hàng đã nhận và đang trong quá trình đi giao.
- **Khoảng cách & Lộ trình**: Nếu truyền `lat` và `lon`, hệ thống sẽ tính khoảng cách thực tế (km) đến **Cửa hàng đích (Store)**.
- **Thông tin chi tiết**: Mỗi mục trong danh sách bao gồm:
    - `distance`: Khoảng cách thực tế đến cửa hàng (km).
    - `storeName`, `storeAddress`: Tên và địa chỉ chi tiết nơi giao hàng.
    - `storeLatitude`, `storeLongitude`: Tọa độ cửa hàng để mở bản đồ (Google Maps).

---

### Bước 3.6: Xem chi tiết một vận đơn

**Endpoint:**
- **Method**: `GET`
- **URL**: `/api/shipper/deliveries/{deliveryId}`
- **Quyền**: `SHIPPER_DELIVERY_VIEW`

**Mục đích:**
- Tra cứu nhanh thông tin chi tiết của một vận đơn cụ thể, bao gồm đầy đủ địa chỉ và tọa độ cửa hàng.

---

### Bước 4: Shipper báo đã giao hàng thành công (Chờ cửa hàng xác nhận)

**Endpoint:**
- **Method**: `PATCH`
- **URL**: `/api/shipper/deliveries/{deliveryId}/mark-success`
- **Quyền**: `SHIPPER_DELIVERY_UPDATE`
- **Body (Tùy chọn)**:
```json
{
  "notes": "Đã bàn giao hàng tại cửa hàng"
}
```

---

### Bước 5: Nhân viên cửa hàng xác nhận đã nhận hàng

**Endpoint:**
- **Method**: `POST`
- **URL**: `/api/store/orders/{orderId}/confirm-receipt`
- **Quyền**: `STORE_ORDER_UPDATE`

**Mô tả:**
- Nhân viên tại cửa hàng kiểm tra hàng hóa và bấm nút xác nhận trên Dashboard của cửa hàng.
- Thao tác này sẽ chuyển trạng thái của cả đơn hàng (Order) và vận đơn (Delivery) sang `DELIVERED`.
