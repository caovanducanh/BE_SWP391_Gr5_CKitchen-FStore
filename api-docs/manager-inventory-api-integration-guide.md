# Manager Inventory API Integration Guide (FE)

Tai lieu nay danh cho Frontend tich hop nhom API manager quan ly ton kho bep trung tam.

Pham vi:
- Danh sach ton kho theo nhom kitchen (kitchen chua list items).
- Tao / cap nhat / xoa item ton kho.
- Lay danh sach kitchen, ingredient, supplier de build filter/dropdown.

---

## 1) Base URL, Auth, Response Wrapper

## 1.1 Base URL
- Local: `http://localhost:8080`
- Production: theo bien moi truong FE (`VITE_API_BASE_URL`)

## 1.2 Authentication
Tat ca endpoint ben duoi deu can JWT Bearer token.

Header mau:
```http
Authorization: Bearer <access_token>
```

## 1.3 Success response wrapper
```json
{
  "statusCode": 200,
  "message": "...",
  "data": {}
}
```

## 1.4 Error response wrapper
```json
{
  "statusCode": 400,
  "message": "...",
  "data": null
}
```

Tham chieu them: `api-docs/error-response.md`.

---

## 2) Permission Matrix

- `INVENTORY_VIEW`
  - Xem danh sach ton kho manager
  - Xem danh sach kitchen
  - Xem danh sach ingredient
  - Xem danh sach supplier

- `INVENTORY_MANAGE`
  - Tao item ton kho
  - Cap nhat item ton kho
  - Xoa item ton kho

Luu y: endpoint duoc bao ve boi `@SecuredEndpoint`, thieu quyen se tra `403`.

---

## 3) Endpoint Overview (Prefix: `/api/manager/inventory`)

- `GET /kitchen`: danh sach ton kho theo nhom kitchen (phan trang)
- `POST /kitchen`: tao item ton kho
- `PUT /kitchen/{id}`: cap nhat item ton kho
- `DELETE /kitchen/{id}`: xoa item ton kho
- `GET /kitchens`: lay toan bo kitchen
- `GET /ingredients`: lay ingredient + unit chuan
- `GET /suppliers`: lay supplier distinct tu du lieu ton kho

---

## 4) API Details

## 4.1 GET /api/manager/inventory/kitchen

- Permission: `INVENTORY_VIEW`
- Query params:
  - `kitchenId` (optional)
  - `lowStock` (optional): `true` hoac `false`
  - `page` (default `0`)
  - `size` (default `20`)

### Logic quan trong
- Response la danh sach **kitchen group**, moi group co `items[]`.
- Pagination ap dung tren so luong kitchen group, **khong** ap dung tren so luong item trong tung kitchen.
- Neu khong truyen `kitchenId`: backend lay tat ca kitchen, sau do loai bo kitchen co `items` rong.
- `lowStock=true`: chi lay item co `quantity <= minStock`.
- `lowStock=false`: chi lay item co `quantity > minStock`.

### Response data shape
```json
{
  "content": [
    {
      "kitchenId": "KIT001",
      "kitchenName": "Central Kitchen HCM",
      "items": [
        {
          "id": 1,
          "ingredientId": "ING001",
          "ingredientName": "Bột mì",
          "quantity": 4,
          "unit": "kg",
          "minStock": 10,
          "batchNo": "KINV-001",
          "expiryDate": null,
          "supplier": "Cong ty Bot Mi",
          "updatedAt": "2026-04-16T11:27:48.484321",
          "lowStock": true
        }
      ]
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### Error thuong gap
- `404 Kitchen not found with id: <kitchenId>` (khi truyen kitchenId khong ton tai)

---

## 4.2 POST /api/manager/inventory/kitchen

- Permission: `INVENTORY_MANAGE`
- Body: `KitchenInventoryUpsertRequest`

```json
{
  "kitchenId": "KIT001",
  "ingredientId": "ING001",
  "quantity": 25,
  "minStock": 10,
  "batchNo": "BATCH-APR-001",
  "expiryDate": "2026-05-15",
  "supplier": "ABC Supplier"
}
```

### Validation
- `kitchenId`: bat buoc, toi da 10 ky tu
- `ingredientId`: bat buoc, toi da 10 ky tu
- `quantity`: bat buoc, `>= 0`
- `minStock`: bat buoc, `>= 0`
- `batchNo`: optional, toi da 30 ky tu
- `expiryDate`: optional (`yyyy-MM-dd`)
- `supplier`: optional, toi da 100 ky tu

### Unit handling (important)
- Request **khong can gui unit**.
- Backend tu lay `unit` chuan tu ingredient va save vao `kitchen_inventory.unit`.
- FE can lay unit chuan qua API `GET /api/manager/inventory/ingredients`.

### Response data shape
`data` la `KitchenInventoryResponse`:
- `id`
- `kitchenId`
- `kitchenName`
- `ingredientId`
- `ingredientName`
- `quantity`
- `unit`
- `minStock`
- `batchNo`
- `expiryDate`
- `supplier`
- `updatedAt`
- `lowStock`

### Error thuong gap
- `404 Kitchen not found with id: ...`
- `404 Ingredient not found with id: ...`
- `400` loi validate body

---

## 4.3 PUT /api/manager/inventory/kitchen/{id}

- Permission: `INVENTORY_MANAGE`
- Path param:
  - `id`: inventory id
- Body: giong `KitchenInventoryUpsertRequest`

### Response
`data` la `KitchenInventoryResponse` sau khi cap nhat.

### Error thuong gap
- `404 Kitchen inventory not found with id: ...`
- `404 Kitchen not found with id: ...`
- `404 Ingredient not found with id: ...`
- `400` loi validate body

---

## 4.4 DELETE /api/manager/inventory/kitchen/{id}

- Permission: `INVENTORY_MANAGE`
- Path param:
  - `id`: inventory id

### Response
- Message: `Kitchen inventory item deleted successfully`
- `data` thuong la `null`

### Error thuong gap
- `404 Kitchen inventory not found with id: ...`

---

## 4.5 GET /api/manager/inventory/kitchens

- Permission: `INVENTORY_VIEW`
- Response: `data` la array `KitchenResponse`

`KitchenResponse` fields:
- `id`, `name`, `address`, `phone`, `capacity`, `status`, `createdAt`, `updatedAt`

---

## 4.6 GET /api/manager/inventory/ingredients

- Permission: `INVENTORY_VIEW`
- Response: `data` la array `IngredientFilterOptionResponse`

`IngredientFilterOptionResponse` fields:
- `id`
- `name`
- `unit`  <- day la unit chuan FE nen dung khi hien thi form create/update

---

## 4.7 GET /api/manager/inventory/suppliers

- Permission: `INVENTORY_VIEW`
- Response: `data` la array `string`
- Nguon du lieu: supplier distinct trong bang `kitchen_inventory`, sap xep tang dan.

---

## 5) FE Integration Flow (de noi nhanh)

1. Load dropdown kitchen: `GET /api/manager/inventory/kitchens`.
2. Load dropdown ingredient + unit chuan: `GET /api/manager/inventory/ingredients`.
3. (Optional) Load dropdown supplier: `GET /api/manager/inventory/suppliers`.
4. Load grid ton kho: `GET /api/manager/inventory/kitchen?...`.
5. Khi tao/sua item:
   - Gui `kitchenId`, `ingredientId`, `quantity`, `minStock`, ...
   - **Khong gui unit**.
   - Hien thi unit tren UI tu ingredient da chon.

---

## 6) Permission Mapping theo Endpoint

| Endpoint | Permission |
|---|---|
| `GET /api/manager/inventory/kitchen` | `INVENTORY_VIEW` |
| `POST /api/manager/inventory/kitchen` | `INVENTORY_MANAGE` |
| `PUT /api/manager/inventory/kitchen/{id}` | `INVENTORY_MANAGE` |
| `DELETE /api/manager/inventory/kitchen/{id}` | `INVENTORY_MANAGE` |
| `GET /api/manager/inventory/kitchens` | `INVENTORY_VIEW` |
| `GET /api/manager/inventory/ingredients` | `INVENTORY_VIEW` |
| `GET /api/manager/inventory/suppliers` | `INVENTORY_VIEW` |

---

## 7) FE Checklist

- [ ] Luon gui `Authorization: Bearer <token>`.
- [ ] Parse response theo `statusCode/message/data`.
- [ ] O list API `/kitchen`, hieu dung data: kitchen group -> `items[]`.
- [ ] Khong hardcode unit tay; luon lay unit tu `/ingredients`.
- [ ] Payload create/update khong gui field `unit`.
- [ ] Xu ly 404 cho kitchen/ingredient/inventory khong ton tai.
