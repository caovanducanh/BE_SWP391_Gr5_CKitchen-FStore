package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.manager.KitchenInventoryUpsertRequest;

import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.manager.IngredientFilterOptionResponse;
import com.example.demologin.dto.response.manager.ManagerKitchenInventoryGroupResponse;
import com.example.demologin.dto.response.manager.ManagerKitchenInventoryItemResponse;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.service.ManagerInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerInventoryServiceImpl implements ManagerInventoryService {

    private final KitchenInventoryRepository kitchenInventoryRepository;
    private final IngredientRepository ingredientRepository;
    private final KitchenRepository kitchenRepository;

    @Override
    public Page<ManagerKitchenInventoryGroupResponse> getKitchenInventory(
            String kitchenId,
            Boolean lowStock,
            int page,
            int size
    ) {
        String normalizedKitchenId = normalizeText(kitchenId);

        List<Kitchen> kitchens;
        if (normalizedKitchenId != null) {
            kitchens = List.of(
                    kitchenRepository.findById(normalizedKitchenId)
                            .orElseThrow(() -> new NotFoundException("Kitchen not found with id: " + normalizedKitchenId))
            );
        } else {
            kitchens = kitchenRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        }

        List<ManagerKitchenInventoryGroupResponse> grouped = kitchens.stream()
                .map(kitchen -> toKitchenGroupResponse(kitchen, lowStock))
                .filter(group -> !group.getItems().isEmpty())
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        int start = Math.min(safePage * safeSize, grouped.size());
        int end = Math.min(start + safeSize, grouped.size());

        return new PageImpl<>(
                grouped.subList(start, end),
                PageRequest.of(safePage, safeSize),
                grouped.size()
        );
    }

    @Override
    public List<KitchenResponse> getAllKitchens() {
        return kitchenRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(this::toKitchenResponse)
                .toList();
    }

    @Override
    public List<IngredientFilterOptionResponse> getAllIngredientsForFilter() {
        return ingredientRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(ingredient -> IngredientFilterOptionResponse.builder()
                        .id(ingredient.getId())
                        .name(ingredient.getName())
                        .unit(ingredient.getUnit())
                        .build())
                .toList();
    }

    @Override
    public List<String> getAllSuppliersForFilter() {
        return List.of(); // Not tracked in KitchenInventory anymore
    }



    private KitchenResponse toKitchenResponse(Kitchen kitchen) {
        return KitchenResponse.builder()
                .id(kitchen.getId())
                .name(kitchen.getName())
                .address(kitchen.getAddress())
                .phone(kitchen.getPhone())
                .capacity(kitchen.getCapacity())
                .status(kitchen.getStatus())
                .createdAt(kitchen.getCreatedAt())
                .updatedAt(kitchen.getUpdatedAt())
                .build();
    }

            private ManagerKitchenInventoryGroupResponse toKitchenGroupResponse(Kitchen kitchen, Boolean lowStock) {
            Specification<KitchenInventory> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("kitchen").get("id"), kitchen.getId())
            );

            if (Boolean.TRUE.equals(lowStock)) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("totalQuantity"), cb.toBigDecimal(root.get("minStock"))));
            } else if (Boolean.FALSE.equals(lowStock)) {
                spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("totalQuantity"), cb.toBigDecimal(root.get("minStock"))));
            }

            List<ManagerKitchenInventoryItemResponse> items = kitchenInventoryRepository
                .findAll(spec, Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream()
                .map(this::toKitchenItemResponse)
                .toList();

            return ManagerKitchenInventoryGroupResponse.builder()
                .kitchenId(kitchen.getId())
                .kitchenName(kitchen.getName())
                .items(items)
                .build();
            }

            private ManagerKitchenInventoryItemResponse toKitchenItemResponse(KitchenInventory inventory) {
            return ManagerKitchenInventoryItemResponse.builder()
                .id(inventory.getId())
                .ingredientId(inventory.getIngredient().getId())
                .ingredientName(inventory.getIngredient().getName())
                .totalQuantity(inventory.getTotalQuantity())
                .unit(inventory.getUnit())
                .minStock(inventory.getMinStock())
                .updatedAt(inventory.getUpdatedAt())
                .lowStock(inventory.getTotalQuantity() != null
                    && inventory.getMinStock() != null
                    && inventory.getTotalQuantity().compareTo(BigDecimal.valueOf(inventory.getMinStock())) <= 0)
                .build();
            }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
