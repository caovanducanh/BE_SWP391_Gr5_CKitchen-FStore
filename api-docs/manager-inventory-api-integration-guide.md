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

---

## 8) API Support Matrix theo Tung Tac Vu FE

Bang nay tra loi truc tiep: "manager can goi API nao de ho tro create/update/filter/quan ly?"

| Tac vu FE | API can goi | Bat buoc/Optional | Muc dich |
|---|---|---|---|
| Load trang manager inventory | `GET /api/manager/inventory/kitchens` | Bat buoc | Lay dropdown kitchen filter |
| Load trang manager inventory | `GET /api/manager/inventory/ingredients` | Bat buoc | Lay ingredient + unit chuan cho form create/update |
| Load trang manager inventory | `GET /api/manager/inventory/suppliers` | Optional | Lay danh sach supplier de filter UI |
| Load grid mac dinh | `GET /api/manager/inventory/kitchen?page=0&size=20` | Bat buoc | Lay data grid dang grouped theo kitchen |
| Filter theo kitchen | `GET /api/manager/inventory/kitchen?kitchenId=KIT001&page=0&size=20` | Bat buoc neu co kitchen filter | Loc du lieu theo kitchen |
| Filter low stock | `GET /api/manager/inventory/kitchen?lowStock=true&page=0&size=20` | Optional | Loc item sap can bo sung |
| Create item | `POST /api/manager/inventory/kitchen` | Bat buoc | Tao item ton kho moi |
| Update item | `PUT /api/manager/inventory/kitchen/{id}` | Bat buoc | Cap nhat item ton kho |
| Delete item | `DELETE /api/manager/inventory/kitchen/{id}` | Bat buoc | Xoa item ton kho |
| Refresh grid sau create/update/delete | Goi lai `GET /api/manager/inventory/kitchen?...` theo filter hien tai | Bat buoc | Dong bo UI voi du lieu moi |

---

## 9) Call Flow Chuan Cho Tung Chuc Nang

## 9.1 Flow Load Trang

1. Goi song song:
   - `GET /api/manager/inventory/kitchens`
   - `GET /api/manager/inventory/ingredients`
   - `GET /api/manager/inventory/suppliers` (neu UI co supplier filter)
2. Goi grid lan dau:
   - `GET /api/manager/inventory/kitchen?page=0&size=20`

Ket qua mong doi:
- Dropdown kitchen co du lieu
- Dropdown ingredient co `id/name/unit`
- Grid ton kho render theo nhom kitchen -> items

## 9.2 Flow Create Item Ton Kho

API ho tro bat buoc truoc khi bam Save:
- `GET /api/manager/inventory/kitchens`
- `GET /api/manager/inventory/ingredients`

Thu tu xu ly FE:
1. User chon `kitchenId` tu dropdown kitchen.
2. User chon `ingredientId` tu dropdown ingredient.
3. FE hien thi unit read-only theo ingredient da chon (`unit` lay tu API ingredients).
4. FE submit:

```json
POST /api/manager/inventory/kitchen
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

5. Sau khi 200, FE goi lai grid theo filter hien tai:
   - `GET /api/manager/inventory/kitchen?...`

Luu y:
- Khong gui field `unit` trong payload create.
- Neu gui kitchenId/ingredientId khong ton tai -> 404.

## 9.3 Flow Update Item Ton Kho

API ho tro bat buoc:
- `GET /api/manager/inventory/ingredients` de cap nhat dropdown ingredient va unit chuan.

Thu tu xu ly FE:
1. Tu item tren grid lay `id` de update.
2. User chinh sua cac truong cho phep: kitchen, ingredient, quantity, minStock, batchNo, expiryDate, supplier.
3. FE submit:

```json
PUT /api/manager/inventory/kitchen/{id}
{
  "kitchenId": "KIT001",
  "ingredientId": "ING002",
  "quantity": 18,
  "minStock": 8,
  "batchNo": "BATCH-APR-002",
  "expiryDate": "2026-06-01",
  "supplier": "ABC Supplier"
}
```

4. Sau khi 200, goi lai list:
   - `GET /api/manager/inventory/kitchen?...`

Luu y:
- Khong gui field `unit` trong payload update.
- Neu `{id}` khong ton tai -> 404 Kitchen inventory not found.

## 9.4 Flow Delete Item

Thu tu xu ly FE:
1. User bam delete tren item co `id`.
2. Goi:
   - `DELETE /api/manager/inventory/kitchen/{id}`
3. Sau khi 200, refresh list:
   - `GET /api/manager/inventory/kitchen?...`

## 9.5 Flow Filter

Manager inventory list hien chi support filter:
- `kitchenId`
- `lowStock`
- `page`, `size`

Vi du:
- Theo kitchen: `GET /api/manager/inventory/kitchen?kitchenId=KIT001&page=0&size=20`
- Theo low stock: `GET /api/manager/inventory/kitchen?lowStock=true&page=0&size=20`
- Ket hop: `GET /api/manager/inventory/kitchen?kitchenId=KIT001&lowStock=true&page=0&size=20`

Luu y quan trong:
- API list hien KHONG support query `ingredientId`, `ingredientName`, `supplier`.
- Neu FE muon loc UI theo ingredient/supplier thi can loc client-side tren du lieu `items[]` sau khi nhan response.

---

## 10) Payload Contract Cho Form Create/Update

`KitchenInventoryUpsertRequest` chinh xac hien tai:

```json
{
  "kitchenId": "string (required, max 10)",
  "ingredientId": "string (required, max 10)",
  "quantity": "number >= 0 (required)",
  "minStock": "integer >= 0 (required)",
  "batchNo": "string (optional, max 30)",
  "expiryDate": "yyyy-MM-dd (optional)",
  "supplier": "string (optional, max 100)"
}
```

Khong co field `unit` trong request.

---

## 11) FE Error Handling Nhanh

- `400`:
  - Loi validate input (`quantity`, `minStock`, max length, ...)
- `403`:
  - Thieu permission (`INVENTORY_VIEW` hoac `INVENTORY_MANAGE`)
- `404`:
  - Kitchen/Ingredient/Inventory item khong ton tai

Khuyen nghi UX:
1. Neu `403`: hien thong bao "Ban khong co quyen thao tac ton kho".
2. Neu `404` sau khi update/delete: dong modal + refresh list de tranh stale data.
3. Neu `400`: hien loi ngay duoi field theo message backend.
