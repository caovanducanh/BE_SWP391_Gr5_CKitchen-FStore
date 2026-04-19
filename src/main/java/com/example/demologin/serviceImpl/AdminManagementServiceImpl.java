package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.admin.KitchenUpsertRequest;
import com.example.demologin.dto.request.admin.OrderPriorityConfigRequest;
import com.example.demologin.dto.request.admin.StoreUpsertRequest;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.dto.response.admin.OrderPriorityConfigResponse;
import com.example.demologin.dto.response.admin.SystemOverviewResponse;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.OrderPriorityConfig;
import com.example.demologin.entity.Store;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.BusinessException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.OrderPriorityConfigRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.RoleRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

    private final OrderPriorityConfigRepository orderPriorityConfigRepository;
    private final StoreRepository storeRepository;
    private final KitchenRepository kitchenRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    public List<OrderPriorityConfigResponse> getOrderPriorityConfigs() {
        List<OrderPriorityConfig> configs = orderPriorityConfigRepository.findAll();
        return configs.stream().map(this::toOrderPriorityConfigResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderPriorityConfigResponse createOrderPriorityConfig(OrderPriorityConfigRequest request) {
        validatePriorityRange(request);
        String priorityCode = normalizePriorityCode(request.getPriorityCode());
        if (orderPriorityConfigRepository.existsByPriorityCode(priorityCode)) {
            throw new BusinessException("Priority code already exists: " + priorityCode);
        }

        OrderPriorityConfig saved = orderPriorityConfigRepository.save(OrderPriorityConfig.builder()
                .priorityCode(priorityCode)
                .minDays(request.getMinDays())
                .maxDays(request.getMaxDays())
                .description(request.getDescription())
                .build());

        return toOrderPriorityConfigResponse(saved);
    }

    @Override
    @Transactional
    public OrderPriorityConfigResponse updateOrderPriorityConfig(Integer id, OrderPriorityConfigRequest request) {
        validatePriorityRange(request);
        String priorityCode = normalizePriorityCode(request.getPriorityCode());

        OrderPriorityConfig config = orderPriorityConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order priority config with id " + id + " not found"));

        orderPriorityConfigRepository.findByPriorityCode(priorityCode)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Priority code already exists: " + priorityCode);
                });

        config.setPriorityCode(priorityCode);
        config.setMinDays(request.getMinDays());
        config.setMaxDays(request.getMaxDays());
        config.setDescription(request.getDescription());

        return toOrderPriorityConfigResponse(orderPriorityConfigRepository.save(config));
    }

    @Override
    public Page<StoreResponse> getStores(String name, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedStatus = normalizeStatus(status);
        String keyword = null;
        if (name != null) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                keyword = trimmed;
            }
        }
        boolean hasName = keyword != null;

        Page<Store> stores;
        if (hasName && normalizedStatus != null) {
            stores = storeRepository.findByNameContainingIgnoreCaseAndStatus(keyword, normalizedStatus, pageable);
        } else if (hasName) {
            stores = storeRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else if (normalizedStatus != null) {
            stores = storeRepository.findByStatus(normalizedStatus, pageable);
        } else {
            stores = storeRepository.findAll(pageable);
        }

        return stores.map(this::toStoreResponse);
    }

    @Override
    public StoreResponse getStoreById(String id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Store with id " + id + " not found"));
        return toStoreResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse createStore(StoreUpsertRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new BadRequestException("Store id must not be blank");
        }
        String storeId = request.getId().trim();
        if (storeRepository.existsById(storeId)) {
            throw new BusinessException("Store id already exists: " + storeId);
        }

        LocalDateTime now = LocalDateTime.now();
        Store saved = storeRepository.save(Store.builder()
                .id(storeId)
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .manager(request.getManager())
                .status(normalizeStatus(request.getStatus()))
                .openDate(request.getOpenDate())
                .createdAt(now)
                .updatedAt(now)
                .build());

        return toStoreResponse(saved);
    }

    @Override
    @Transactional
    public StoreResponse updateStore(String id, StoreUpsertRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Store with id " + id + " not found"));

        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setPhone(request.getPhone());
        store.setManager(request.getManager());
        store.setStatus(normalizeStatus(request.getStatus()));
        store.setOpenDate(request.getOpenDate());
        store.setUpdatedAt(LocalDateTime.now());

        return toStoreResponse(storeRepository.save(store));
    }

    @Override
    @Transactional
    public StoreResponse updateStoreStatus(String id, String status) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Store with id " + id + " not found"));

        store.setStatus(normalizeStatus(status));
        store.setUpdatedAt(LocalDateTime.now());
        return toStoreResponse(storeRepository.save(store));
    }

    @Override
    public Page<KitchenResponse> getKitchens(String name, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedStatus = normalizeStatus(status);
        String keyword = null;
        if (name != null) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                keyword = trimmed;
            }
        }
        boolean hasName = keyword != null;

        Page<Kitchen> kitchens;
        if (hasName && normalizedStatus != null) {
            kitchens = kitchenRepository.findByNameContainingIgnoreCaseAndStatus(keyword, normalizedStatus, pageable);
        } else if (hasName) {
            kitchens = kitchenRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else if (normalizedStatus != null) {
            kitchens = kitchenRepository.findByStatus(normalizedStatus, pageable);
        } else {
            kitchens = kitchenRepository.findAll(pageable);
        }

        return kitchens.map(this::toKitchenResponse);
    }

    @Override
    public KitchenResponse getKitchenById(String id) {
        Kitchen kitchen = kitchenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kitchen with id " + id + " not found"));
        return toKitchenResponse(kitchen);
    }

    @Override
    @Transactional
    public KitchenResponse createKitchen(KitchenUpsertRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new BadRequestException("Kitchen id must not be blank");
        }
        String kitchenId = request.getId().trim();
        if (kitchenRepository.existsById(kitchenId)) {
            throw new BusinessException("Kitchen id already exists: " + kitchenId);
        }

        LocalDateTime now = LocalDateTime.now();
        Kitchen saved = kitchenRepository.save(Kitchen.builder()
                .id(kitchenId)
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .capacity(request.getCapacity())
                .status(normalizeStatus(request.getStatus()))
                .createdAt(now)
                .updatedAt(now)
                .build());

        return toKitchenResponse(saved);
    }

    @Override
    @Transactional
    public KitchenResponse updateKitchen(String id, KitchenUpsertRequest request) {
        Kitchen kitchen = kitchenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kitchen with id " + id + " not found"));

        kitchen.setName(request.getName());
        kitchen.setAddress(request.getAddress());
        kitchen.setPhone(request.getPhone());
        kitchen.setCapacity(request.getCapacity());
        kitchen.setStatus(normalizeStatus(request.getStatus()));
        kitchen.setUpdatedAt(LocalDateTime.now());

        return toKitchenResponse(kitchenRepository.save(kitchen));
    }

    @Override
    @Transactional
    public KitchenResponse updateKitchenStatus(String id, String status) {
        Kitchen kitchen = kitchenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kitchen with id " + id + " not found"));

        kitchen.setStatus(normalizeStatus(status));
        kitchen.setUpdatedAt(LocalDateTime.now());
        return toKitchenResponse(kitchenRepository.save(kitchen));
    }

    @Override
    public SystemOverviewResponse getSystemOverview(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        Map<String, Long> orderStatusCounts = new LinkedHashMap<>();
        Arrays.stream(OrderStatus.values()).forEach(status ->
                orderStatusCounts.put(status.name(), countOrdersByStatusWithDateFilter(status, fromDateTime, toDateTime))
        );

        long totalOrders = countOrdersWithDateFilter(fromDateTime, toDateTime);

        return SystemOverviewResponse.builder()
                .totalUsers(userRepository.count())
                .totalRoles(roleRepository.count())
                .totalStores(storeRepository.count())
                .activeStores(storeRepository.countByStatus("ACTIVE"))
                .totalKitchens(kitchenRepository.count())
                .activeKitchens(kitchenRepository.countByStatus("ACTIVE"))
                .totalProducts(productRepository.count())
                .totalOrders(totalOrders)
                .orderStatusCounts(orderStatusCounts)
                .build();
    }

    private long countOrdersWithDateFilter(LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (fromDateTime != null && toDateTime != null) {
            return orderRepository.countByCreatedAtBetween(fromDateTime, toDateTime);
        }
        if (fromDateTime != null) {
            return orderRepository.countByCreatedAtGreaterThanEqual(fromDateTime);
        }
        if (toDateTime != null) {
            return orderRepository.countByCreatedAtLessThanEqual(toDateTime);
        }
        return orderRepository.count();
    }

    private long countOrdersByStatusWithDateFilter(OrderStatus status, LocalDateTime fromDateTime, LocalDateTime toDateTime) {
        if (fromDateTime != null && toDateTime != null) {
            return orderRepository.countByStatusAndCreatedAtBetween(status, fromDateTime, toDateTime);
        }
        if (fromDateTime != null) {
            return orderRepository.countByStatusAndCreatedAtGreaterThanEqual(status, fromDateTime);
        }
        if (toDateTime != null) {
            return orderRepository.countByStatusAndCreatedAtLessThanEqual(status, toDateTime);
        }
        return orderRepository.countByStatus(status);
    }

    private OrderPriorityConfigResponse toOrderPriorityConfigResponse(OrderPriorityConfig config) {
        return OrderPriorityConfigResponse.builder()
                .id(config.getId())
                .priorityCode(config.getPriorityCode())
                .minDays(config.getMinDays())
                .maxDays(config.getMaxDays())
                .description(config.getDescription())
                .build();
    }

    private StoreResponse toStoreResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .phone(store.getPhone())
                .manager(store.getManager())
                .status(store.getStatus())
                .openDate(store.getOpenDate())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
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

    private void validatePriorityRange(OrderPriorityConfigRequest request) {
        if (request.getMinDays() == null) {
            throw new BadRequestException("minDays is required");
        }
        if (request.getMaxDays() != null && request.getMaxDays() < request.getMinDays()) {
            throw new BadRequestException("maxDays must be greater than or equal to minDays");
        }
    }

    private String normalizePriorityCode(String priorityCode) {
        return priorityCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized)) {
            throw new BadRequestException("Status must be ACTIVE or INACTIVE");
        }
        return normalized;
    }
}
