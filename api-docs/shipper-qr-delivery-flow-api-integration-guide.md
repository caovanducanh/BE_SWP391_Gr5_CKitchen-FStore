# Shipper QR Delivery Flow API Integration Guide (FE)

Tai lieu nay mo ta day du luong giao nhan moi:
- Dieu phoi vien sinh QR nhan don theo order
- QR duoc dan len thung hang de shipper quet
- He thong tu sinh deliveryId (ma van don) neu chua co
- Shipper danh dau da giao thanh cong (cho cua hang xac nhan)
- Store staff xac nhan da nhan don de chot don hoan tat

Muc tieu: FE noi API theo dung thu tu, de hieu, khong can doan nghiep vu.

---

## 1) Tong quan va pham vi

- Prefix Supply Coordinator: /api/supply-coordinator
- Prefix Shipper: /api/shipper
- Prefix Store Staff: /api/store
- Auth: JWT Bearer token
- Content-Type: application/json

Response thanh cong:

```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

Response loi:

```json
{
  "statusCode": 400,
  "message": "...",
  "data": null
}
```

---

## 2) Trang thai chinh can map o FE

### 2.1) Delivery status
- ASSIGNED
- SHIPPING
- DELAYED
- WAITING_CONFIRM
- DELIVERED
- CANCELLED

### 2.2) Order status lien quan luong giao nhan
- PACKED_WAITING_SHIPPER
- SHIPPING
- DELIVERED
- CANCELLED

### 2.3) Y nghia moc thoi gian
- pickedUpAt: luc shipper nhan don sau khi quet QR
- deliveredAt: luc shipper bao da giao thanh cong (mark-success)
- Store confirm KHONG ghi de deliveredAt neu da co

---

## 3) End-to-end flow cho FE

## Buoc 1: Dieu phoi vien sinh QR theo order

Endpoint:
- Method: GET
- URL: /api/supply-coordinator/orders/{orderId}/pickup-qr
- Permission: SUPPLY_DELIVERY_VIEW

Muc dich:
- Lay ma QR de dan len thung hang
- Neu order chua co delivery thi backend tu tao delivery
- deliveryId duoc sinh tai buoc nay neu can

Dieu kien sinh moi delivery:
- Order status phai la PACKED_WAITING_SHIPPER hoac SHIPPING

Response data:

```json
{
  "orderId": "ORD002",
  "deliveryId": "DEL0421008",
  "pickupQrCode": "PK-ORD002-3D451097",
  "deliveryStatus": "ASSIGNED"
}
```

Goi y FE:
- In pickupQrCode thanh QR image
- Hien deliveryId tren label van don
- Luu mapping orderId <-> deliveryId de tra cuu nhanh

---

## Buoc 2: (Optional) Shipper xem don dang cho nhan

Endpoint:
- Method: GET
- URL: /api/shipper/orders/available?page=0&size=20
- Permission: SHIPPER_DELIVERY_VIEW

Muc dich:
- Hien list don da dong goi, co delivery va chua co shipper cam

---

## Buoc 3: Shipper quet QR de nhan don

Endpoint:
- Method: POST
- URL: /api/shipper/deliveries/scan-qr
- Permission: SHIPPER_DELIVERY_CLAIM
- Body:

```json
{
  "qrCode": "PK-ORD002-3D451097"
}
```

Backend cap nhat:
- delivery.shipper = current shipper
- delivery.pickedUpAt = now (neu chua co)
- delivery.status = SHIPPING
- order.status = SHIPPING
- order.shippingAt = now (neu chua co)

---

## Buoc 4: Shipper bao da giao thanh cong (cho store confirm)

Endpoint:
- Method: PATCH
- URL: /api/shipper/deliveries/{deliveryId}/mark-success
- Permission: SHIPPER_DELIVERY_UPDATE
- Body (optional):

```json
{
  "notes": "Da ban giao tai cua hang"
}
```

Backend cap nhat:
- delivery.status = WAITING_CONFIRM
- delivery.deliveredAt = now (neu chua co)
- order.status = SHIPPING (van cho store staff xac nhan)

Response data se co deliveredAt da duoc set.

---

## Buoc 5: Store staff xac nhan da nhan don

Co 2 cach goi API, tuy context man hinh FE:

### Cach A - Xac nhan theo order detail
- Method: POST
- URL: /api/store/orders/{orderId}/confirm-receipt
- Permission: DELIVERY_CONFIRM

### Cach B - Xac nhan theo delivery
- Method: POST
- URL: /api/store/deliveries/{deliveryId}/confirm
- Permission: DELIVERY_CONFIRM

Body (bat buoc):

```json
{
  "receiverName": "Nguyen Van A",
  "temperatureOk": true,
  "notes": "Nhan du so luong"
}
```

Dieu kien backend cho xac nhan:
- delivery.status phai la SHIPPING hoac WAITING_CONFIRM

Backend cap nhat:
- delivery.status = DELIVERED
- order.status = DELIVERED
- deliveredAt giu nguyen neu da co tu buoc mark-success

---

## 4) API tra cuu ai dang cam don (holder)

### 4.1) Cho dieu phoi vien
- Method: GET
- URL: /api/supply-coordinator/orders/{orderId}/holder
- Permission: SUPPLY_DELIVERY_VIEW

### 4.2) Cho shipper
- Method: GET
- URL: /api/shipper/orders/{orderId}/holder
- Permission: SHIPPER_DELIVERY_VIEW

Response data:

```json
{
  "orderId": "ORD002",
  "deliveryId": "DEL0421008",
  "orderStatus": "SHIPPING",
  "deliveryStatus": "WAITING_CONFIRM",
  "pickupQrCode": "PK-ORD002-3D451097",
  "holderUserId": 12,
  "holderUsername": "shipper",
  "holderFullName": "shipper Fullname",
  "pickedUpAt": "2026-04-21T09:43:19.234103"
}
```

---

## 5) Sequence goi API de FE noi nhanh

1. Supply UI: GET /api/supply-coordinator/orders/{orderId}/pickup-qr
2. In/dan QR + hien deliveryId tren thung hang
3. Shipper app: POST /api/shipper/deliveries/scan-qr
4. Shipper app: PATCH /api/shipper/deliveries/{deliveryId}/mark-success
5. Store app: POST /api/store/orders/{orderId}/confirm-receipt (hoac /deliveries/{deliveryId}/confirm)

---

## 6) Loi thuong gap va cach xu ly FE

### 6.1) 400 Cannot generate pickup QR when order status is ...
Nguyen nhan:
- Order chua den PACKED_WAITING_SHIPPER/SHIPPING

Xu ly FE:
- Disable nut Sinh QR khi order status khong hop le

### 6.2) 400 This order is already claimed by another shipper
Nguyen nhan:
- QR da duoc shipper khac quet nhan

Xu ly FE:
- Show modal don da co nguoi nhan, refresh holder

### 6.3) 400 Delivery is not ready for store confirmation
Nguyen nhan:
- Store confirm sai trang thai (khong phai SHIPPING/WAITING_CONFIRM)

Xu ly FE:
- Chi hien thi nut Xac nhan nhan hang khi status hop le

### 6.4) 404 Delivery not found for order
Nguyen nhan:
- orderId sai hoac order chua co delivery

Xu ly FE:
- Goi lai API pickup-qr de tao delivery neu order du dieu kien

---

## 7) Checklist UI/UX de de van hanh

- Hien thi ca orderId, deliveryId, pickupQrCode tren man hinh dieu phoi
- Co nut Copy QR code text de backup khi camera loi
- Sau scan thanh cong, refresh holder panel ngay
- Store dashboard co tab WAITING_CONFIRM de xu ly nhanh
- Khi store confirm xong, disable nut confirm tranh double submit

---

## 8) Tinh tuong thich

Flow nay da backward-compatible:
- Store van co the confirm tu SHIPPING
- Neu da co delivery truoc do, pickup-qr se tai su dung delivery hien tai va bo sung QR neu thieu
