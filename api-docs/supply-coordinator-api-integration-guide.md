# Supply Coordinator API Integration Guide (FE)

Tai lieu nay danh cho Frontend tich hop API cho role `SUPPLY_COORDINATOR`.

Muc tieu: FE co the tong hop don, dieu phoi bep, lap lich giao hang va xu ly su co nhanh theo dung flow van hanh.

---

## 1) Tong quan

- **Prefix API**: `/api/supply-coordinator`
- **Content-Type**: `application/json`
- **Auth**: JWT Bearer token
  - `Authorization: Bearer <access_token>`

> Toan bo endpoint trong tai lieu nay yeu cau login + dung permission cua Supply Coordinator.

---

## 2) Quy uoc response

Response thanh cong duoc wrap theo format chung:

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

API list su dung `page` + `size`, du lieu tra ve trong `data.content` va cac truong paging.

---

## 3) Danh sach endpoint

## 3.1) Tong hop va phan loai don hang

- **Method**: `GET`
- **URL**: `/api/supply-coordinator/orders`
- **Permission**: `SUPPLY_ORDER_VIEW`
- **Query**:
  - `status` (optional)
  - `priority` (optional)
  - `storeId` (optional)
  - `kitchenId` (optional)
  - `fromDate` (optional, yyyy-MM-dd)
  - `toDate` (optional, yyyy-MM-dd)
  - `page` (default=0)
  - `size` (default=20)

---

## 3.2) Chi tiet don hang

- **Method**: `GET`
- **URL**: `/api/supply-coordinator/orders/{orderId}`
- **Permission**: `SUPPLY_ORDER_VIEW`

---

## 3.3) Dieu phoi don sang bep

- **Method**: `PATCH`
- **URL**: `/api/supply-coordinator/orders/{orderId}/assign-kitchen`
- **Permission**: `SUPPLY_ORDER_ASSIGN`
- **Body**:

```json
{
  "kitchenId": "KIT001",
  "notes": "Dieu phoi cho bep trung tam 1"
}
```

---

## 3.4) Tong quan dieu phoi

- **Method**: `GET`
- **URL**: `/api/supply-coordinator/overview`
- **Permission**: `SUPPLY_ORDER_VIEW`
- **Query**:
  - `fromDate` (optional)
  - `toDate` (optional)

Response `data` gom cac chi so chinh:
- `totalOrders`
- `pendingOrders`
- `assignedOrders`
- `inProgressOrders`
- `packedWaitingShipperOrders`
- `shippingOrders`
- `deliveredOrders`
- `cancelledOrders`
- `overdueOrders`
- `unassignedOrders`
- `activeDeliveries`

---

## 3.5) Lap lich giao hang

- **Method**: `POST`
- **URL**: `/api/supply-coordinator/deliveries`
- **Permission**: `SUPPLY_DELIVERY_SCHEDULE`
- **Body**:

```json
{
  "orderId": "ORD0419001",
  "status": "ASSIGNED",
  "assignedAt": "2026-04-19T10:30:00",
  "notes": "Uu tien giao truoc 12h"
}
```

`status` cho luc tao chi nhan: `ASSIGNED`, `SHIPPING`.

---

## 3.6) Theo doi tien do giao hang

- **Method**: `GET`
- **URL**: `/api/supply-coordinator/deliveries`
- **Permission**: `SUPPLY_DELIVERY_VIEW`
- **Query**:
  - `status` (optional): `ASSIGNED`, `SHIPPING`, `DELAYED`, `DELIVERED`, `CANCELLED`
  - `page` (default=0)
  - `size` (default=20)

---

## 3.7) Cap nhat trang thai giao hang

- **Method**: `PATCH`
- **URL**: `/api/supply-coordinator/deliveries/{deliveryId}/status`
- **Permission**: `SUPPLY_DELIVERY_UPDATE`
- **Body**:

```json
{
  "status": "SHIPPING",
  "notes": "Da roi kho luc 09:00"
}
```

Status hop le:
- `ASSIGNED`
- `SHIPPING`
- `DELAYED`
- `DELIVERED`
- `CANCELLED`

---

## 3.8) Xu ly su co phat sinh

- **Method**: `POST`
- **URL**: `/api/supply-coordinator/orders/{orderId}/issues`
- **Permission**: `SUPPLY_ISSUE_MANAGE`
- **Body**:

```json
{
  "issueType": "DELAY",
  "description": "Xe gap su co tren duong Nguyen Van Linh",
  "cancelOrder": false
}
```

`issueType` hop le:
- `SHORTAGE`
- `DELAY`
- `CANCELLATION`
- `OTHER`

Neu `cancelOrder=true` hoac `issueType=CANCELLATION`, backend se huy don.

---

## 3.9) Danh sach trang thai cho UI

- `GET /api/supply-coordinator/order-statuses`
  - Permission: `SUPPLY_ORDER_VIEW`
- `GET /api/supply-coordinator/delivery-statuses`
  - Permission: `SUPPLY_DELIVERY_VIEW`

---

## 4) Flow FE khuyen nghi

1. Goi `GET /overview` de hien thi dashboard dieu phoi.
2. Tai man hinh xu ly don: goi `GET /orders` va filter theo status/priority.
3. Don chua gan bep: goi `PATCH /orders/{orderId}/assign-kitchen`.
4. Don san sang giao: goi `POST /deliveries` de lap lich.
5. Theo doi giao van bang `GET /deliveries`, cap nhat tien do qua `PATCH /deliveries/{deliveryId}/status`.
6. Khi phat sinh thieu hang, tre giao, huy don: goi `POST /orders/{orderId}/issues`.
