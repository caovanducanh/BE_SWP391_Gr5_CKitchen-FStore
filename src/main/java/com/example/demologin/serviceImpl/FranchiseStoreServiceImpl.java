package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.store.ConfirmReceiptRequest;
import com.example.demologin.dto.request.store.CreateOrderRequest;
import com.example.demologin.dto.request.store.OrderItemRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderItemResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.dto.response.StoreInventoryResponse;
import com.example.demologin.entity.Delivery;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderItem;
import com.example.demologin.entity.OrderPriorityConfig;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.StoreInventory;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.DeliveryRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderPriorityConfigRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.StoreInventoryRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.FranchiseStoreService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FranchiseStoreServiceImpl implements FranchiseStoreService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreRepository storeRepository;
    private final KitchenRepository kitchenRepository;
    private final ProductRepository productRepository;
    private final OrderPriorityConfigRepository orderPriorityConfigRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, Principal principal) {
        Store userStore = getCurrentStore(principal);
        if (userStore == null) {
            throw new IllegalStateException("Only store staff can create orders");
        }
        Store store = userStore;

        String orderId = generateOrderId();
        String priority = calculatePriority(request.getRequestedDate());

        // Pre-check: Verify all products exist
        for (OrderItemRequest itemReq : request.getItems()) {
            if (!productRepository.existsById(itemReq.getProductId())) {
                throw new NotFoundException("Product not found: " + itemReq.getProductId());
            }
        }

        Order order = Order.builder()
                .id(orderId)
                .store(store)
                .status("PENDING")
                .priority(priority)
                .createdAt(LocalDateTime.now())
                .requestedDate(request.getRequestedDate())
                .notes(request.getNotes())
                .createdBy(principal.getName())
                .updatedAt(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);

        final Order savedOrder = order;
        List<OrderItem> items = request.getItems().stream().map(itemReq -> buildOrderItem(itemReq, savedOrder))
                .collect(Collectors.toList());
        orderItemRepository.saveAll(items);

        return toOrderResponse(savedOrder, items);
    }

    @Override
    public Page<OrderResponse> getOrders(String status, Principal principal, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Store userStore = getCurrentStore(principal);
        if (userStore == null) {
            throw new IllegalStateException("Only store staff can view their orders");
        }
        String finalStoreId = userStore.getId();

        boolean hasStatus = status != null && !status.isBlank();

        Page<Order> orders;
        if (hasStatus) {
            orders = orderRepository.findByStore_IdAndStatus(finalStoreId, status.toUpperCase(), pageRequest);
        } else {
            orders = orderRepository.findByStore_Id(finalStoreId, pageRequest);
        }

        return orders.map(o -> {
            List<OrderItem> items = orderItemRepository.findByOrder_Id(o.getId());
            return toOrderResponse(o, items);
        });
    }

    @Override
    public OrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        return toOrderResponse(order, items);
    }

    @Override
    public DeliveryResponse getDeliveryByOrderId(String orderId) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
        Delivery delivery = deliveryRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new NotFoundException("No delivery found for order: " + orderId));
        return toDeliveryResponse(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponse confirmReceipt(String deliveryId, ConfirmReceiptRequest request) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));

        delivery.setReceiverName(request.getReceiverName());
        delivery.setTemperatureOk(request.getTemperatureOk());
        if (request.getNotes() != null) {
            delivery.setNotes(request.getNotes());
        }
        delivery.setDeliveredAt(LocalDateTime.now());
        delivery.setStatus("DELIVERED");
        delivery.setUpdatedAt(LocalDateTime.now());

        // Update associated order status
        Order order = delivery.getOrder();
        order.setStatus("DELIVERED");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return toDeliveryResponse(deliveryRepository.save(delivery));
    }

    @Override
    public Page<StoreInventoryResponse> getStoreInventory(String productId, String productName, Principal principal, int page, int size) {
        Store userStore = getCurrentStore(principal);
        if (userStore == null) {
            throw new IllegalStateException("Only store staff can view their inventory");
        }
        String finalStoreId = userStore.getId();

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("product.name"));

        Specification<StoreInventory> spec = Specification.where((root, query, cb) -> 
                cb.equal(root.get("store").get("id"), finalStoreId));

        if (productId != null && !productId.isBlank()) {
            if (!productRepository.existsById(productId)) {
                throw new NotFoundException("Product not found: " + productId);
            }
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }

        if (productName != null && !productName.isBlank()) {
            String searchPattern = "%" + productName.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("product").get("name")), searchPattern));
        }

        return storeInventoryRepository.findAll(spec, pageRequest).map(this::toInventoryResponse);
    }

    @Override
    public StoreResponse getMyStore(Principal principal) {
        Store store = getCurrentStore(principal);
        if (store == null) {
            throw new NotFoundException("Current user is not associated with any store");
        }
        return toStoreResponse(store);
    }

    // ==================== Helpers ====================
    private String calculatePriority(LocalDate requestedDate) {
        long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), requestedDate);
        if (daysBetween < 0) {
            daysBetween = 0; // Treat past or today as same for priority
        }

        long finalDaysBetween = daysBetween;
        return orderPriorityConfigRepository.findAll().stream()
                .filter(config -> finalDaysBetween >= config.getMinDays() && (config.getMaxDays() == null || finalDaysBetween <= config.getMaxDays()))
                .map(OrderPriorityConfig::getPriorityCode)
                .findFirst()
                .orElse("NORMAL");
    }

    private OrderItem buildOrderItem(OrderItemRequest req, Order order) {
        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + req.getProductId()));
        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(req.getQuantity())
                .unit(product.getUnit())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String generateOrderId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
        long count = orderRepository.count() + 1;
        return String.format("ORD%s%03d", datePart, count % 1000);
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

        return OrderResponse.builder()
                .id(order.getId())
                .storeId(order.getStore().getId())
                .storeName(order.getStore().getName())
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
                .status(delivery.getStatus())
                .assignedAt(delivery.getAssignedAt())
                .deliveredAt(delivery.getDeliveredAt())
                .notes(delivery.getNotes())
                .receiverName(delivery.getReceiverName())
                .temperatureOk(delivery.getTemperatureOk())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }

    private StoreInventoryResponse toInventoryResponse(StoreInventory inv) {
        return StoreInventoryResponse.builder()
                .id(inv.getId())
                .storeId(inv.getStore().getId())
                .storeName(inv.getStore().getName())
                .productId(inv.getProduct().getId())
                .productName(inv.getProduct().getName())
                .quantity(inv.getQuantity())
                .unit(inv.getUnit())
                .minStock(inv.getMinStock())
                .expiryDate(inv.getExpiryDate())
                .updatedAt(inv.getUpdatedAt())
                .lowStock(inv.getQuantity() <= inv.getMinStock())
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

    private Store getCurrentStore(Principal principal) {
        if (principal == null) return null;
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.getName()));

        if (user.getRole() != null && user.getRole().getName().equalsIgnoreCase("FRANCHISE_STORE_STAFF")) {
            if (user.getStore() == null) {
                throw new IllegalStateException("Store staff is not assigned to any store");
            }
            return user.getStore();
        }
        return null;
    }
}
