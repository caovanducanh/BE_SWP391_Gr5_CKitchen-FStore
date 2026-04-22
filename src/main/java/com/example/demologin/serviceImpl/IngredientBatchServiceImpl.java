package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.centralkitchen.ImportIngredientBatchRequest;
import com.example.demologin.dto.response.IngredientBatchResponse;
import com.example.demologin.dto.response.KitchenInventoryDetailResponse;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.IngredientBatch;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.IngredientBatchRepository;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.IngredientBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientBatchServiceImpl implements IngredientBatchService {

    private final IngredientBatchRepository ingredientBatchRepository;
    private final KitchenInventoryRepository kitchenInventoryRepository;
    private final IngredientRepository ingredientRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public IngredientBatchResponse importBatch(ImportIngredientBatchRequest request, Principal principal) {
        User staff = getKitchenStaff(principal);
        Kitchen kitchen = staff.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Bạn chưa được gán vào bếp nào");
        }

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId().trim())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nguyên liệu: " + request.getIngredientId()));

        String batchNo = request.getBatchNo().trim();

        // Kiểm tra số lô không trùng trong cùng bếp + nguyên liệu
        if (ingredientBatchRepository.existsByKitchen_IdAndIngredient_IdAndBatchNo(
                kitchen.getId(), ingredient.getId(), batchNo)) {
            throw new BadRequestException("Số lô '" + batchNo + "' đã tồn tại cho nguyên liệu này trong bếp");
        }

        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Ngày hết hạn không được nhỏ hơn ngày hiện tại");
        }

        LocalDateTime now = LocalDateTime.now();
        IngredientBatch batch = IngredientBatch.builder()
                .id(generateBatchId())
                .ingredient(ingredient)
                .kitchen(kitchen)
                .batchNo(batchNo)
                .initialQuantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .unit(ingredient.getUnit())
                .expiryDate(request.getExpiryDate())
                .supplier(request.getSupplier())
                .importPrice(request.getImportPrice())
                .importDate(request.getImportDate() != null ? request.getImportDate() : LocalDate.now())
                .status("ACTIVE")
                .notes(request.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .build();

        ingredientBatchRepository.save(batch);

        // Cập nhật tổng tồn kho (upsert KitchenInventory)
        upsertKitchenInventory(kitchen, ingredient, request.getQuantity(), request.getMinStock(), now);

        return toResponse(batch);
    }

    @Override
    public Page<IngredientBatchResponse> getBatches(
            String ingredientId, String ingredientName, String status,
            int page, int size, Principal principal) {

        User staff = getKitchenStaff(principal);
        Kitchen kitchen = staff.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Bạn chưa được gán vào bếp nào");
        }

        Specification<IngredientBatch> spec = Specification.where(
                (root, q, cb) -> cb.equal(root.get("kitchen").get("id"), kitchen.getId())
        );

        if (ingredientId != null && !ingredientId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("ingredient").get("id"), ingredientId.trim()));
        }
        if (ingredientName != null && !ingredientName.isBlank()) {
            String keyword = "%" + ingredientName.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("ingredient").get("name")), keyword));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "expiryDate"));
        return ingredientBatchRepository.findAll(spec, pageRequest).map(this::toResponse);
    }

    @Override
    public IngredientBatchResponse getBatchById(String id, Principal principal) {
        User staff = getKitchenStaff(principal);
        Kitchen kitchen = staff.getKitchen();

        IngredientBatch batch = ingredientBatchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lô nguyên liệu: " + id));

        if (kitchen != null && !batch.getKitchen().getId().equals(kitchen.getId())) {
            throw new BadRequestException("Lô này không thuộc bếp của bạn");
        }
        return toResponse(batch);
    }

    @Override
    public Page<KitchenInventoryDetailResponse> getInventory(
            String ingredientId, String ingredientName,
            Boolean lowStock, int page, int size, Principal principal) {

        User staff = getKitchenStaff(principal);
        Kitchen kitchen = staff.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Bạn chưa được gán vào bếp nào");
        }

        Specification<KitchenInventory> spec = Specification.where(
                (root, q, cb) -> cb.equal(root.get("kitchen").get("id"), kitchen.getId())
        );
        if (ingredientId != null && !ingredientId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("ingredient").get("id"), ingredientId.trim()));
        }
        if (ingredientName != null && !ingredientName.isBlank()) {
            String kw = "%" + ingredientName.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("ingredient").get("name")), kw));
        }
        if (Boolean.TRUE.equals(lowStock)) {
            spec = spec.and((root, q, cb) ->
                    cb.lessThanOrEqualTo(root.get("totalQuantity"), cb.toBigDecimal(root.get("minStock"))));
        } else if (Boolean.FALSE.equals(lowStock)) {
            spec = spec.and((root, q, cb) ->
                    cb.greaterThan(root.get("totalQuantity"), cb.toBigDecimal(root.get("minStock"))));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "ingredient.name"));
        Page<KitchenInventory> inventoryPage = kitchenInventoryRepository.findAll(spec, pageRequest);

        List<KitchenInventoryDetailResponse> content = inventoryPage.getContent().stream()
                .map(inv -> toDetailResponse(inv, kitchen.getId()))
                .toList();

        return new PageImpl<>(content, pageRequest, inventoryPage.getTotalElements());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void upsertKitchenInventory(Kitchen kitchen, Ingredient ingredient,
                                        BigDecimal addedQty, Integer minStockOverride,
                                        LocalDateTime now) {
        kitchenInventoryRepository
                .findByKitchen_IdAndIngredient_Id(kitchen.getId(), ingredient.getId())
                .ifPresentOrElse(inv -> {
                    inv.setTotalQuantity(inv.getTotalQuantity().add(addedQty));
                    inv.setUpdatedAt(now);
                    kitchenInventoryRepository.save(inv);
                }, () -> {
                    int minStock = minStockOverride != null ? minStockOverride : ingredient.getMinStock();
                    kitchenInventoryRepository.save(KitchenInventory.builder()
                            .ingredient(ingredient)
                            .kitchen(kitchen)
                            .totalQuantity(addedQty)
                            .unit(ingredient.getUnit())
                            .minStock(minStock)
                            .updatedAt(now)
                            .build());
                });
    }

    private KitchenInventoryDetailResponse toDetailResponse(KitchenInventory inv, String kitchenId) {
        List<IngredientBatchResponse> batches = ingredientBatchRepository
                .findActiveByKitchenAndIngredientOrderByExpiryAsc(kitchenId, inv.getIngredient().getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return KitchenInventoryDetailResponse.builder()
                .id(inv.getId())
                .kitchenId(inv.getKitchen().getId())
                .kitchenName(inv.getKitchen().getName())
                .ingredientId(inv.getIngredient().getId())
                .ingredientName(inv.getIngredient().getName())
                .totalQuantity(inv.getTotalQuantity())
                .unit(inv.getUnit())
                .minStock(inv.getMinStock())
                .lowStock(inv.getTotalQuantity().compareTo(BigDecimal.valueOf(inv.getMinStock())) <= 0)
                .batches(batches)
                .build();
    }

    private IngredientBatchResponse toResponse(IngredientBatch b) {
        return IngredientBatchResponse.builder()
                .id(b.getId())
                .kitchenId(b.getKitchen().getId())
                .kitchenName(b.getKitchen().getName())
                .ingredientId(b.getIngredient().getId())
                .ingredientName(b.getIngredient().getName())
                .batchNo(b.getBatchNo())
                .initialQuantity(b.getInitialQuantity())
                .remainingQuantity(b.getRemainingQuantity())
                .unit(b.getUnit())
                .expiryDate(b.getExpiryDate())
                .supplier(b.getSupplier())
                .importPrice(b.getImportPrice())
                .importDate(b.getImportDate())
                .status(b.getStatus())
                .notes(b.getNotes())
                .nearExpiry(b.getExpiryDate() != null && !b.getExpiryDate().isAfter(LocalDate.now().plusDays(30)))
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    private User getKitchenStaff(Principal principal) {
        if (principal == null) throw new IllegalStateException("Unauthenticated");
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.getName()));
    }

    private String generateBatchId() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        return "IB" + ts;
    }
}
