package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.supplycoordinator.AssignOrderKitchenRequest;
import com.example.demologin.dto.request.supplycoordinator.HandleIssueRequest;
import com.example.demologin.dto.request.supplycoordinator.ScheduleDeliveryRequest;
import com.example.demologin.dto.request.supplycoordinator.UpdateDeliveryStatusRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.OrderHolderResponse;
import com.example.demologin.dto.response.OrderItemResponse;
import com.example.demologin.dto.response.OrderPickupQrResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.SupplyCoordinatorOverviewResponse;
import com.example.demologin.entity.Delivery;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderItem;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.User;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.DeliveryRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.SupplyCoordinatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplyCoordinatorServiceImpl implements SupplyCoordinatorService {

    private static final String ROLE_SUPPLY_COORDINATOR = "SUPPLY_COORDINATOR";

    private static final Set<String> DELIVERY_STATUSES = Set.of(
            "ASSIGNED",
            "SHIPPING",
            "DELAYED",
            "WAITING_CONFIRM",
            "DELIVERED",
            "CANCELLED"
    );

    private static final Set<String> ALLOWED_INITIAL_DELIVERY_STATUSES = Set.of(
            "ASSIGNED",
            "SHIPPING"
    );

    private static final Set<String> ISSUE_TYPES = Set.of(
            "SHORTAGE",
            "DELAY",
            "CANCELLATION",
            "OTHER"
    );

    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.ASSIGNED,
            OrderStatus.IN_PROGRESS,
            OrderStatus.PACKED_WAITING_SHIPPER,
            OrderStatus.SHIPPING,
            OrderStatus.APPROVED,
            OrderStatus.PROCESSING
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final KitchenRepository kitchenRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Override
    public Page<KitchenResponse> getKitchens(int page, int size, Principal principal) {
        validateSupplyCoordinator(principal);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return kitchenRepository.findAll(pageRequest)
                .map(this::toKitchenResponse);
    }

    @Override
    public Page<OrderResponse> getOrders(String status,
                                         String priority,
                                         String storeId,
                                         String kitchenId,
                                         LocalDate fromDate,
                                         LocalDate toDate,
                                         int page,
                                         int size,
                                         Principal principal) {
        validateSupplyCoordinator(principal);
        validateDateRange(fromDate, toDate);

        OrderStatus normalizedStatus = parseOrderStatus(status);
        String normalizedPriority = normalizeText(priority);
        String normalizedStoreId = normalizeText(storeId);
        String normalizedKitchenId = normalizeText(kitchenId);

        if (normalizedStoreId != null && !storeRepository.existsById(normalizedStoreId)) {
            throw new NotFoundException("Store not found: " + normalizedStoreId);
        }

        if (normalizedKitchenId != null && !kitchenRepository.existsById(normalizedKitchenId)) {
            throw new NotFoundException("Kitchen not found: " + normalizedKitchenId);
        }

        Specification<Order> spec = Specification.where(null);

        if (normalizedStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalizedStatus));
        }

        if (normalizedPriority != null) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("priority")), normalizedPriority.toUpperCase()));
        }

        if (normalizedStoreId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("store").get("id"), normalizedStoreId));
        }

        if (normalizedKitchenId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("kitchen").get("id"), normalizedKitchenId));
        }

        spec = spec.and(requestedDateBetween(fromDate, toDate));

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findAll(spec, pageRequest)
                .map(order -> toOrderResponse(order, orderItemRepository.findByOrder_Id(order.getId())));
    }

    @Override
    public OrderResponse getOrderById(String orderId, Principal principal) {
        validateSupplyCoordinator(principal);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        return toOrderResponse(order, orderItemRepository.findByOrder_Id(orderId));
    }

    @Override
    @Transactional
    public OrderResponse assignOrderToKitchen(String orderId, AssignOrderKitchenRequest request, Principal principal) {
        User coordinator = getCurrentSupplyCoordinator(principal);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        Kitchen kitchen = kitchenRepository.findById(request.getKitchenId())
                .orElseThrow(() -> new NotFoundException("Kitchen not found: " + request.getKitchenId()));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot assign kitchen for completed or cancelled order: " + orderId);
        }

        order.setKitchen(kitchen);
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.ASSIGNED);
        }
        if (order.getAssignedAt() == null) {
            order.setAssignedAt(LocalDateTime.now());
        }

        String normalizedNote = normalizeText(request.getNotes());
        if (normalizedNote != null) {
            order.setNotes(appendNote(order.getNotes(), "SC-ASSIGN", coordinator.getUsername(), normalizedNote));
        }

        order.setUpdatedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        return toOrderResponse(saved, orderItemRepository.findByOrder_Id(saved.getId()));
    }

    @Override
    public SupplyCoordinatorOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate, Principal principal) {
        User coordinator = getCurrentSupplyCoordinator(principal);
        validateDateRange(fromDate, toDate);

        Specification<Order> baseSpec = requestedDateBetween(fromDate, toDate);

        long totalOrders = orderRepository.count(baseSpec);
        long pendingOrders = orderRepository.count(baseSpec.and(hasStatus(OrderStatus.PENDING)));
        long assignedOrders = orderRepository.count(baseSpec.and(hasStatus(OrderStatus.ASSIGNED)));
        long inProgressOrders = orderRepository.count(baseSpec.and(hasStatuses(List.of(OrderStatus.IN_PROGRESS, OrderStatus.PROCESSING))));
        long packedWaitingShipperOrders = orderRepository.count(baseSpec.and(hasStatus(OrderStatus.PACKED_WAITING_SHIPPER)));
        long shippingOrders = orderRepository.count(baseSpec.and(hasStatus(OrderStatus.SHIPPING)));
        long deliveredOrders = orderRepository.count(baseSpec.and(hasStatus(OrderStatus.DELIVERED)));
        long cancelledOrders = orderRepository.count(baseSpec.and(hasStatus(OrderStatus.CANCELLED)));
        long overdueOrders = orderRepository.count(baseSpec
                .and(hasStatuses(List.copyOf(ACTIVE_ORDER_STATUSES)))
                .and(requestedDateBefore(LocalDate.now())));
        long unassignedOrders = orderRepository.count(baseSpec
                .and(hasStatus(OrderStatus.PENDING))
                .and(kitchenIsNull()));

        long activeDeliveries = deliveryRepository.countByCoordinator_UserIdAndStatusIn(
                coordinator.getUserId(),
                List.of("ASSIGNED", "SHIPPING", "DELAYED")
        );

        return SupplyCoordinatorOverviewResponse.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .assignedOrders(assignedOrders)
                .inProgressOrders(inProgressOrders)
                .packedWaitingShipperOrders(packedWaitingShipperOrders)
                .shippingOrders(shippingOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .overdueOrders(overdueOrders)
                .unassignedOrders(unassignedOrders)
                .activeDeliveries(activeDeliveries)
                .build();
    }

    @Override
    @Transactional
    public DeliveryResponse scheduleDelivery(ScheduleDeliveryRequest request, Principal principal) {
        User coordinator = getCurrentSupplyCoordinator(principal);

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + request.getOrderId()));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot schedule delivery for order status: " + order.getStatus());
        }

        deliveryRepository.findByOrder_Id(order.getId()).ifPresent(existing -> {
            throw new BadRequestException("Delivery already exists for order: " + order.getId());
        });

        String normalizedStatus = normalizeText(request.getStatus());
        String deliveryStatus = normalizedStatus == null ? "ASSIGNED" : normalizedStatus.toUpperCase();
        if (!ALLOWED_INITIAL_DELIVERY_STATUSES.contains(deliveryStatus)) {
            throw new BadRequestException("Invalid initial delivery status: " + deliveryStatus);
        }

        LocalDateTime assignedAt = request.getAssignedAt() != null ? request.getAssignedAt() : LocalDateTime.now();

        Delivery delivery = Delivery.builder()
                .id(generateDeliveryId())
                .order(order)
                .coordinator(coordinator)
                .status(deliveryStatus)
                .assignedAt(assignedAt)
            .pickupQrCode(generatePickupQrCode(order.getId()))
                .notes(normalizeText(request.getNotes()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (order.getAssignedAt() == null) {
            order.setAssignedAt(assignedAt);
        }
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.ASSIGNED);
        }

        if ("SHIPPING".equals(deliveryStatus)) {
            order.setStatus(OrderStatus.SHIPPING);
            if (order.getShippingAt() == null) {
                order.setShippingAt(LocalDateTime.now());
            }
        }

        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return toDeliveryResponse(deliveryRepository.save(delivery));
    }

    @Override
    public Page<DeliveryResponse> getDeliveries(String status, int page, int size, Principal principal) {
        User coordinator = getCurrentSupplyCoordinator(principal);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));

        String normalizedStatus = normalizeText(status);
        if (normalizedStatus == null) {
            return deliveryRepository.findByCoordinator_UserId(coordinator.getUserId(), pageRequest)
                    .map(this::toDeliveryResponse);
        }

        String deliveryStatus = normalizedStatus.toUpperCase();
        if (!DELIVERY_STATUSES.contains(deliveryStatus)) {
            throw new BadRequestException("Invalid delivery status: " + status);
        }

        return deliveryRepository.findByCoordinator_UserIdAndStatus(coordinator.getUserId(), deliveryStatus, pageRequest)
                .map(this::toDeliveryResponse);
    }

    @Override
    @Transactional
    public DeliveryResponse updateDeliveryStatus(String deliveryId, UpdateDeliveryStatusRequest request, Principal principal) {
        User coordinator = getCurrentSupplyCoordinator(principal);

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));

        String nextStatus = normalizeText(request.getStatus());
        if (nextStatus == null) {
            throw new BadRequestException("status is required");
        }

        String normalizedStatus = nextStatus.toUpperCase();
        if (!DELIVERY_STATUSES.contains(normalizedStatus)) {
            throw new BadRequestException("Invalid delivery status: " + request.getStatus());
        }

        Order order = delivery.getOrder();
        LocalDateTime now = LocalDateTime.now();

        delivery.setStatus(normalizedStatus);
        String note = normalizeText(request.getNotes());
        if (note != null) {
            delivery.setNotes(appendNote(delivery.getNotes(), "SC-DELIVERY", coordinator.getUsername(), note));
        }

        String receiverName = normalizeText(request.getReceiverName());
        if (receiverName != null) {
            delivery.setReceiverName(receiverName);
        }
        if (request.getTemperatureOk() != null) {
            delivery.setTemperatureOk(request.getTemperatureOk());
        }

        switch (normalizedStatus) {
            case "ASSIGNED" -> {
                if (order.getAssignedAt() == null) {
                    order.setAssignedAt(now);
                }
                if (order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.ASSIGNED);
                }
            }
            case "SHIPPING" -> {
                if (order.getAssignedAt() == null) {
                    order.setAssignedAt(now);
                }
                if (order.getShippingAt() == null) {
                    order.setShippingAt(now);
                }
                order.setStatus(OrderStatus.SHIPPING);
            }
            case "DELAYED" ->
                    order.setNotes(appendNote(order.getNotes(), "SC-ISSUE", coordinator.getUsername(), "Delivery delayed: " + delivery.getId()));
            case "DELIVERED" -> {
                if (delivery.getDeliveredAt() == null) {
                    delivery.setDeliveredAt(now);
                }
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(now);
                }
                order.setStatus(OrderStatus.DELIVERED);
            }
            case "CANCELLED" -> {
                if (order.getCancelledAt() == null) {
                    order.setCancelledAt(now);
                }
                order.setStatus(OrderStatus.CANCELLED);
            }
            default -> {
                // no-op
            }
        }

        delivery.setUpdatedAt(now);
        order.setUpdatedAt(now);

        orderRepository.save(order);
        return toDeliveryResponse(deliveryRepository.save(delivery));
    }

        @Override
        public OrderPickupQrResponse getOrderPickupQr(String orderId, Principal principal) {
            User coordinator = getCurrentSupplyCoordinator(principal);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

            Delivery delivery = deliveryRepository.findByOrder_Id(orderId).orElse(null);

            if (delivery == null) {
                if (order.getStatus() != OrderStatus.PACKED_WAITING_SHIPPER && order.getStatus() != OrderStatus.SHIPPING) {
                    throw new BadRequestException("Cannot generate pickup QR when order status is " + order.getStatus());
                }

                LocalDateTime now = LocalDateTime.now();
                String deliveryStatus = order.getStatus() == OrderStatus.SHIPPING ? "SHIPPING" : "ASSIGNED";

                delivery = Delivery.builder()
                        .id(generateDeliveryId())
                        .order(order)
                        .coordinator(coordinator)
                        .status(deliveryStatus)
                        .assignedAt(now)
                        .pickupQrCode(generatePickupQrCode(orderId))
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                delivery = deliveryRepository.save(delivery);
            }

        if (delivery.getPickupQrCode() == null || delivery.getPickupQrCode().isBlank()) {
            delivery.setPickupQrCode(generatePickupQrCode(orderId));
            delivery.setUpdatedAt(LocalDateTime.now());
            delivery = deliveryRepository.save(delivery);
        }

        return OrderPickupQrResponse.builder()
            .orderId(order.getId())
            .deliveryId(delivery.getId())
            .pickupQrCode(delivery.getPickupQrCode())
            .deliveryStatus(delivery.getStatus())
            .build();
        }

        @Override
        public OrderHolderResponse getOrderHolder(String orderId, Principal principal) {
        validateSupplyCoordinator(principal);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        Delivery delivery = deliveryRepository.findByOrder_Id(orderId)
            .orElseThrow(() -> new NotFoundException("Delivery not found for order: " + orderId));

        return toOrderHolderResponse(order, delivery);
        }

    @Override
    @Transactional
    public OrderResponse handleIssue(String orderId, HandleIssueRequest request, Principal principal) {
        User coordinator = getCurrentSupplyCoordinator(principal);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        String issueType = normalizeText(request.getIssueType());
        if (issueType == null) {
            throw new BadRequestException("issueType is required");
        }

        String normalizedIssueType = issueType.toUpperCase();
        if (!ISSUE_TYPES.contains(normalizedIssueType)) {
            throw new BadRequestException("Unsupported issue type: " + issueType);
        }

        String description = normalizeText(request.getDescription());
        if (description == null) {
            throw new BadRequestException("description is required");
        }

        order.setNotes(appendNote(order.getNotes(), "SC-ISSUE", coordinator.getUsername(),
                "[" + normalizedIssueType + "] " + description));

        boolean shouldCancel = Boolean.TRUE.equals(request.getCancelOrder()) || "CANCELLATION".equals(normalizedIssueType);
        LocalDateTime now = LocalDateTime.now();

        Delivery delivery = deliveryRepository.findByOrder_Id(order.getId()).orElse(null);

        if ("DELAY".equals(normalizedIssueType) && delivery != null && !"DELIVERED".equalsIgnoreCase(delivery.getStatus())) {
            delivery.setStatus("DELAYED");
            delivery.setNotes(appendNote(delivery.getNotes(), "SC-ISSUE", coordinator.getUsername(), description));
            delivery.setUpdatedAt(now);
            deliveryRepository.save(delivery);
        }

        if (shouldCancel) {
            order.setStatus(OrderStatus.CANCELLED);
            if (order.getCancelledAt() == null) {
                order.setCancelledAt(now);
            }
            if (delivery != null && !"DELIVERED".equalsIgnoreCase(delivery.getStatus())) {
                delivery.setStatus("CANCELLED");
                delivery.setUpdatedAt(now);
                deliveryRepository.save(delivery);
            }
        }

        order.setUpdatedAt(now);
        Order saved = orderRepository.save(order);
        return toOrderResponse(saved, orderItemRepository.findByOrder_Id(saved.getId()));
    }

    @Override
    public List<String> getOrderStatuses(Principal principal) {
        validateSupplyCoordinator(principal);
        return List.of(
                OrderStatus.PENDING.name(),
                OrderStatus.ASSIGNED.name(),
                OrderStatus.IN_PROGRESS.name(),
                OrderStatus.PACKED_WAITING_SHIPPER.name(),
                OrderStatus.SHIPPING.name(),
                OrderStatus.DELIVERED.name(),
                OrderStatus.CANCELLED.name()
        );
    }

    @Override
    public List<String> getDeliveryStatuses(Principal principal) {
        validateSupplyCoordinator(principal);
        return DELIVERY_STATUSES.stream().sorted().collect(Collectors.toList());
    }

    private void validateSupplyCoordinator(Principal principal) {
        getCurrentSupplyCoordinator(principal);
    }

    private User getCurrentSupplyCoordinator(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.getName()));

        if (user.getRole() == null || !ROLE_SUPPLY_COORDINATOR.equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only supply coordinator can access this resource");
        }

        return user;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }
    }

    private OrderStatus parseOrderStatus(String status) {
        String normalized = normalizeText(status);
        if (normalized == null) {
            return null;
        }

        try {
            return OrderStatus.valueOf(normalized.toUpperCase());
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

    private Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private Specification<Order> hasStatuses(List<OrderStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    private Specification<Order> kitchenIsNull() {
        return (root, query, cb) -> cb.isNull(root.get("kitchen"));
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

    private String appendNote(String oldNotes, String source, String username, String note) {
        String line = "[" + source + " " + username + " - "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + "] " + note;
        if (oldNotes == null || oldNotes.isBlank()) {
            return line;
        }
        return oldNotes + "\n" + line;
    }

    private String generateDeliveryId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
        long count = deliveryRepository.count() + 1;
        return String.format("DEL%s%03d", datePart, count % 1000);
    }

    private String generatePickupQrCode(String orderId) {
        return "PK-" + orderId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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

    private DeliveryResponse toDeliveryResponse(Delivery delivery) {
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrder().getId())
                .coordinatorName(delivery.getCoordinator() != null ? delivery.getCoordinator().getFullName() : null)
                .shipperName(delivery.getShipper() != null ? delivery.getShipper().getFullName() : null)
                .status(delivery.getStatus())
                .assignedAt(delivery.getAssignedAt())
                .pickedUpAt(delivery.getPickedUpAt())
                .deliveredAt(delivery.getDeliveredAt())
                .pickupQrCode(delivery.getPickupQrCode())
                .notes(delivery.getNotes())
                .receiverName(delivery.getReceiverName())
                .temperatureOk(delivery.getTemperatureOk())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
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

    private OrderHolderResponse toOrderHolderResponse(Order order, Delivery delivery) {
        User holder = delivery.getShipper();
        return OrderHolderResponse.builder()
                .orderId(order.getId())
                .deliveryId(delivery.getId())
                .orderStatus(order.getStatus())
                .deliveryStatus(delivery.getStatus())
                .pickupQrCode(delivery.getPickupQrCode())
                .holderUserId(holder != null ? holder.getUserId() : null)
                .holderUsername(holder != null ? holder.getUsername() : null)
                .holderFullName(holder != null ? holder.getFullName() : null)
                .pickedUpAt(delivery.getPickedUpAt())
                .build();
    }
}
