package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.CentralKitchenOverviewResponse;
import com.example.demologin.dto.response.KitchenInventoryResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.OrderItemResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.ProductionPlanResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderItem;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.ProductionPlan;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.User;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.ProductionPlanRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.CentralKitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentralKitchenServiceImpl implements CentralKitchenService {

    private static final Set<OrderStatus> ALLOWED_ORDER_STATUSES = Set.of(
        OrderStatus.PENDING,
        OrderStatus.ASSIGNED,
        OrderStatus.IN_PROGRESS,
        OrderStatus.PACKED_WAITING_SHIPPER,
        OrderStatus.SHIPPING,
        OrderStatus.DELIVERED,
        OrderStatus.CANCELLED,
        // legacy support
        OrderStatus.PROCESSING,
        OrderStatus.APPROVED
    );

    private static final Map<OrderStatus, Set<OrderStatus>> STATUS_TRANSITIONS = Map.of(
        OrderStatus.PENDING, Set.of(OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED),
        OrderStatus.ASSIGNED, Set.of(OrderStatus.IN_PROGRESS, OrderStatus.CANCELLED),
        OrderStatus.IN_PROGRESS, Set.of(OrderStatus.PACKED_WAITING_SHIPPER, OrderStatus.CANCELLED),
        OrderStatus.PACKED_WAITING_SHIPPER, Set.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED),
        OrderStatus.SHIPPING, Set.of(OrderStatus.DELIVERED),
        // legacy transitions for old data rows
        OrderStatus.PROCESSING, Set.of(OrderStatus.PACKED_WAITING_SHIPPER, OrderStatus.SHIPPING, OrderStatus.CANCELLED),
        OrderStatus.APPROVED, Set.of(OrderStatus.IN_PROGRESS, OrderStatus.SHIPPING, OrderStatus.CANCELLED)
    );

        private static final List<OrderStatus> UI_UPDATE_STATUSES = List.of(
            OrderStatus.IN_PROGRESS,
            OrderStatus.PACKED_WAITING_SHIPPER,
            OrderStatus.SHIPPING,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED
        );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StoreRepository storeRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final ProductRepository productRepository;
    private final KitchenInventoryRepository kitchenInventoryRepository;
    private final UserRepository userRepository;

    @Override
    public Page<OrderResponse> getAllOrders(String status, String storeId, int page, int size, Principal principal) {
        validateCentralKitchenStaff(principal);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        OrderStatus normalizedStatus = parseOrderStatus(status);
        String normalizedStoreId = normalizeText(storeId);

        if (normalizedStatus != null && !ALLOWED_ORDER_STATUSES.contains(normalizedStatus)) {
            throw new BadRequestException("Invalid order status: " + normalizedStatus);
        }

        if (normalizedStoreId != null && !storeRepository.existsById(normalizedStoreId)) {
            throw new NotFoundException("Store not found: " + normalizedStoreId);
        }

        Page<Order> orders;
        if (normalizedStatus != null && normalizedStoreId != null) {
            orders = orderRepository.findByStore_IdAndStatus(normalizedStoreId, normalizedStatus, pageRequest);
        } else if (normalizedStatus != null) {
            orders = orderRepository.findByStatus(normalizedStatus, pageRequest);
        } else if (normalizedStoreId != null) {
            orders = orderRepository.findByStore_Id(normalizedStoreId, pageRequest);
        } else {
            orders = orderRepository.findAll(pageRequest);
        }

        return orders.map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
            return toOrderResponse(order, items);
        });
    }

    @Override
    public OrderResponse getOrderById(String orderId, Principal principal) {
        validateCentralKitchenStaff(principal);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        return toOrderResponse(order, items);
    }

    @Override
    @Transactional
    public OrderResponse assignOrder(String orderId, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Kitchen kitchen = currentUser.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Central kitchen staff is not assigned to any kitchen");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus != OrderStatus.PENDING && currentStatus != OrderStatus.ASSIGNED) {
            throw new BadRequestException("Order cannot be assigned from status: " + order.getStatus());
        }

        order.setKitchen(kitchen);
        order.setStatus(OrderStatus.ASSIGNED);
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrder_Id(saved.getId());
        return toOrderResponse(saved, items);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request, Principal principal) {
        validateCentralKitchenStaff(principal);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        if (!ALLOWED_ORDER_STATUSES.contains(newStatus)) {
            throw new BadRequestException("Invalid order status: " + newStatus);
        }

        if (!currentStatus.equals(newStatus)) {
            Set<OrderStatus> allowedNextStatuses = STATUS_TRANSITIONS.get(currentStatus);
            if (allowedNextStatuses == null || !allowedNextStatuses.contains(newStatus)) {
                throw new BadRequestException(
                        "Invalid status transition from " + currentStatus + " to " + newStatus
                );
            }
            order.setStatus(newStatus);
        }

        String normalizedNotes = normalizeText(request.getNotes());
        if (normalizedNotes != null) {
            order.setNotes(appendInternalNote(order.getNotes(), principal.getName(), normalizedNotes));
        }

        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrder_Id(saved.getId());
        return toOrderResponse(saved, items);
    }

    @Override
    public Page<ProductionPlanResponse> getProductionPlans(int page, int size, Principal principal) {
        validateCentralKitchenStaff(principal);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return productionPlanRepository.findAll(pageRequest).map(this::toProductionPlanResponse);
    }

    @Override
    @Transactional
    public ProductionPlanResponse createProductionPlan(CreateProductionPlanRequest request, Principal principal) {
        validateCentralKitchenStaff(principal);

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("endDate must be after startDate");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + request.getProductId()));

        ProductionPlan plan = ProductionPlan.builder()
                .id(generateProductionPlanId())
                .product(product)
                .quantity(request.getQuantity())
                .unit(product.getUnit())
                .status("PLANNED")
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .staff(principal.getName())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return toProductionPlanResponse(productionPlanRepository.save(plan));
    }

    @Override
    public Page<KitchenInventoryResponse> getInventory(String ingredientId, String ingredientName, int page, int size, Principal principal) {
        validateCentralKitchenStaff(principal);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Specification<KitchenInventory> spec = Specification.where(null);

        String normalizedIngredientId = normalizeText(ingredientId);
        if (normalizedIngredientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ingredient").get("id"), normalizedIngredientId));
        }

        String normalizedIngredientName = normalizeText(ingredientName);
        if (normalizedIngredientName != null) {
            String keyword = "%" + normalizedIngredientName.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("ingredient").get("name")), keyword));
        }

        return kitchenInventoryRepository.findAll(spec, pageRequest).map(this::toKitchenInventoryResponse);
    }

    @Override
    public Page<StoreResponse> getStores(String name, String status, int page, int size, Principal principal) {
        validateCentralKitchenStaff(principal);

        String normalizedName = normalizeText(name);
        String normalizedStatus = normalizeText(status);

        Specification<Store> spec = Specification.where(null);
        if (normalizedName != null) {
            String keyword = "%" + normalizedName.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), keyword));
        }
        if (normalizedStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), normalizedStatus.toUpperCase()));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return storeRepository.findAll(spec, pageRequest).map(this::toStoreResponse);
    }

    @Override
    public KitchenResponse getMyKitchen(Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Kitchen kitchen = currentUser.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Central kitchen staff is not assigned to any kitchen");
        }
        return toKitchenResponse(kitchen);
    }

    @Override
    public List<String> getOrderStatuses(Principal principal) {
        validateCentralKitchenStaff(principal);
        return UI_UPDATE_STATUSES.stream()
                .map(Enum::name)
                .toList();
    }

    @Override
    public CentralKitchenOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Kitchen kitchen = currentUser.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Central kitchen staff is not assigned to any kitchen");
        }

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        String kitchenId = kitchen.getId();
        List<OrderStatus> activeStatuses = Arrays.asList(
                OrderStatus.ASSIGNED,
                OrderStatus.IN_PROGRESS,
                OrderStatus.PACKED_WAITING_SHIPPER,
                OrderStatus.SHIPPING,
                OrderStatus.APPROVED,
                OrderStatus.PROCESSING
        );

            Specification<Order> dateFilter = requestedDateBetween(fromDate, toDate);

            long pendingUnassigned = orderRepository.count(
                Specification.where(hasStatus(OrderStatus.PENDING))
                    .and(kitchenIsNull())
                    .and(dateFilter)
            );
            long assignedToMyKitchen = orderRepository.count(
                Specification.where(hasKitchenId(kitchenId))
                    .and(hasStatuses(activeStatuses))
                    .and(dateFilter)
            );
            long inProgress = orderRepository.count(
                Specification.where(hasKitchenId(kitchenId))
                    .and(hasStatuses(List.of(OrderStatus.IN_PROGRESS, OrderStatus.PROCESSING)))
                    .and(dateFilter)
            );
            long packedWaitingShipper = orderRepository.count(
                Specification.where(hasKitchenId(kitchenId))
                    .and(hasStatus(OrderStatus.PACKED_WAITING_SHIPPER))
                    .and(dateFilter)
            );
            long shipping = orderRepository.count(
                Specification.where(hasKitchenId(kitchenId))
                    .and(hasStatus(OrderStatus.SHIPPING))
                    .and(dateFilter)
            );
            long overdue = orderRepository.count(
                Specification.where(hasKitchenId(kitchenId))
                    .and(hasStatuses(activeStatuses))
                    .and(requestedDateBefore(LocalDate.now()))
                    .and(dateFilter)
            );

        return CentralKitchenOverviewResponse.builder()
                .kitchenId(kitchen.getId())
                .kitchenName(kitchen.getName())
                .pendingUnassignedOrders(pendingUnassigned)
                .assignedToMyKitchen(assignedToMyKitchen)
                .inProgressOrders(inProgress)
                .packedWaitingShipperOrders(packedWaitingShipper)
                .shippingOrders(shipping)
                .overdueOrders(overdue)
                .build();
    }

    private Specification<Order> hasKitchenId(String kitchenId) {
        return (root, query, cb) -> cb.equal(root.get("kitchen").get("id"), kitchenId);
    }

    private Specification<Order> kitchenIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("kitchen"));
    }

    private Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<Order> hasStatuses(List<OrderStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    private Specification<Order> requestedDateBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThan(root.get("requestedDate"), date);
    }

    private Specification<Order> requestedDateBetween(LocalDate fromDate, LocalDate toDate) {
        Specification<Order> spec = Specification.where(null);
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("requestedDate"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("requestedDate"), toDate));
        }
        return spec;
    }

    private void validateCentralKitchenStaff(Principal principal) {
        getCurrentCentralKitchenStaff(principal);
    }

    private User getCurrentCentralKitchenStaff(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.getName()));

        if (user.getRole() == null || !"CENTRAL_KITCHEN_STAFF".equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only central kitchen staff can access this resource");
        }

        return user;
    }

    private OrderStatus parseOrderStatus(String status) {
        String value = normalizeText(status);
        if (value == null) {
            return null;
        }
        try {
            return OrderStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid order status: " + status);
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String appendInternalNote(String oldNotes, String updatedBy, String note) {
        String line = "[CK " + updatedBy + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "] " + note;
        if (oldNotes == null || oldNotes.isBlank()) {
            return line;
        }
        return oldNotes + "\n" + line;
    }

    private String generateProductionPlanId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
        long count = productionPlanRepository.count() + 1;
        return String.format("PLN%s%03d", datePart, count % 1000);
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream().map(i -> OrderItemResponse.builder()
                .id(i.getId())
                .productId(i.getProduct().getId())
                .productName(i.getProduct().getName())
                .quantity(i.getQuantity())
                .unit(i.getUnit())
                .createdAt(i.getCreatedAt())
                .build()).collect(Collectors.toList());

        Store store = order.getStore();

        return OrderResponse.builder()
                .id(order.getId())
                .storeId(store != null ? store.getId() : null)
                .storeName(store != null ? store.getName() : null)
                .kitchenId(order.getKitchen() != null ? order.getKitchen().getId() : null)
                .kitchenName(order.getKitchen() != null ? order.getKitchen().getName() : null)
                .status(order.getStatus())
                .priority(order.getPriority())
                .createdAt(order.getCreatedAt())
                .requestedDate(order.getRequestedDate())
                .notes(order.getNotes())
                .createdBy(order.getCreatedBy())
                .total(order.getTotal())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    private ProductionPlanResponse toProductionPlanResponse(ProductionPlan plan) {
        Product product = plan.getProduct();
        return ProductionPlanResponse.builder()
                .id(plan.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .quantity(plan.getQuantity())
                .unit(plan.getUnit())
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .staff(plan.getStaff())
                .notes(plan.getNotes())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private KitchenInventoryResponse toKitchenInventoryResponse(KitchenInventory inventory) {
        return KitchenInventoryResponse.builder()
                .id(inventory.getId())
                .ingredientId(inventory.getIngredient().getId())
                .ingredientName(inventory.getIngredient().getName())
                .quantity(inventory.getQuantity())
                .unit(inventory.getUnit())
                .minStock(inventory.getMinStock())
                .batchNo(inventory.getBatchNo())
                .expiryDate(inventory.getExpiryDate())
                .supplier(inventory.getSupplier())
                .updatedAt(inventory.getUpdatedAt())
                .lowStock(inventory.getQuantity() != null
                        && inventory.getMinStock() != null
                    && inventory.getQuantity().compareTo(BigDecimal.valueOf(inventory.getMinStock())) <= 0)
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
}
