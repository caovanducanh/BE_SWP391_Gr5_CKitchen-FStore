package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.shipper.MarkDeliverySuccessRequest;
import com.example.demologin.dto.request.shipper.ScanPickupQrRequest;
import com.example.demologin.dto.response.DeliveryResponse;
import com.example.demologin.dto.response.OrderHolderResponse;
import com.example.demologin.dto.response.OrderItemResponse;
import com.example.demologin.dto.response.OrderResponse;
import com.example.demologin.entity.Delivery;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderItem;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.User;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.DeliveryRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipperServiceImpl implements ShipperService {

    private static final String ROLE_SHIPPER = "SHIPPER";
    private static final String DELIVERY_WAITING_STORE_CONFIRM = "WAITING_CONFIRM";

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final com.example.demologin.service.GeocodingService geocodingService;

    @Override
    public Page<OrderResponse> getAvailableOrders(int page, int size, Double lat, Double lon, Principal principal) {
        validateShipper(principal);

        if (lat == null || lon == null) {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));
            Page<Delivery> deliveryPage = deliveryRepository.findByOrder_StatusAndShipperIsNull(OrderStatus.PACKED_WAITING_SHIPPER, pageRequest);
            
            List<OrderResponse> content = deliveryPage.stream()
                    .map(delivery -> {
                        try {
                            return toOrderResponse(delivery.getOrder(), orderItemRepository.findByOrder_Id(delivery.getOrder().getId()));
                        } catch (Exception e) {
                            log.error("Error mapping delivery {} to order response: {}", delivery.getId(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            
            return new PageImpl<>(content, pageRequest, deliveryPage.getTotalElements());
        }

        List<Delivery> allDeliveries = deliveryRepository.findByOrder_StatusAndShipperIsNull(OrderStatus.PACKED_WAITING_SHIPPER);
        List<OrderResponse> sortedResponses = allDeliveries.stream()
                .map(delivery -> {
                    try {
                        Order order = delivery.getOrder();
                        OrderResponse response = toOrderResponse(order, orderItemRepository.findByOrder_Id(order.getId()));
                        try {
                            if (order.getKitchen() != null) {
                                Double kLat = order.getKitchen().getLatitude();
                                Double kLon = order.getKitchen().getLongitude();
                                if (kLat != null && kLon != null) {
                                    response.setDistance(geocodingService.calculateDistance(lat, lon, kLat, kLon));
                                }
                            }
                        } catch (Exception distEx) {
                            log.warn("Distance calculation failed for delivery {}: {}", delivery.getId(), distEx.getMessage());
                        }
                        return response;
                    } catch (Exception e) {
                        log.error("Error mapping delivery {} to order response: {}", delivery.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .sorted((r1, r2) -> {
                    if (r1.getDistance() == null && r2.getDistance() == null) return 0;
                    if (r1.getDistance() == null) return 1;
                    if (r2.getDistance() == null) return -1;
                    return r1.getDistance().compareTo(r2.getDistance());
                })
                .collect(Collectors.toList());

        int start = Math.min(page * size, sortedResponses.size());
        int end = Math.min((start + size), sortedResponses.size());
        List<OrderResponse> pagedResponses = sortedResponses.subList(start, end);

        return new PageImpl<>(pagedResponses, PageRequest.of(page, size), sortedResponses.size());
    }

    @Override
    @Transactional
    public DeliveryResponse scanPickupQr(ScanPickupQrRequest request, Principal principal) {
        User shipper = getCurrentShipper(principal);

        String qrCode = normalizeText(request.getQrCode());
        if (qrCode == null) {
            throw new BadRequestException("qrCode is required");
        }

        Delivery delivery = deliveryRepository.findByPickupQrCode(qrCode)
                .orElseThrow(() -> new NotFoundException("Pickup QR not found"));

        Order order;
        try {
            order = delivery.getOrder();
            if (order == null) throw new NotFoundException("Order not found for delivery");
            // Trigger proxy
            order.getId();
        } catch (Exception e) {
            throw new NotFoundException("Order record is missing for this delivery");
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot claim order with status: " + order.getStatus());
        }

        if (delivery.getShipper() != null && !delivery.getShipper().getUserId().equals(shipper.getUserId())) {
            throw new BadRequestException("This order is already claimed by another shipper");
        }

        LocalDateTime now = LocalDateTime.now();

        if (delivery.getShipper() == null) {
            delivery.setShipper(shipper);
        }
        if (delivery.getPickedUpAt() == null) {
            delivery.setPickedUpAt(now);
        }

        delivery.setStatus("SHIPPING");
        delivery.setUpdatedAt(now);

        if (order.getPackedWaitingShipperAt() == null) {
            order.setPackedWaitingShipperAt(now);
        }
        if (order.getShippingAt() == null) {
            order.setShippingAt(now);
        }
        order.setStatus(OrderStatus.SHIPPING);
        order.setUpdatedAt(now);
        order.setNotes(appendNote(order.getNotes(), "SHIPPER", shipper.getUsername(), "Claimed by QR: " + qrCode));

        deliveryRepository.save(delivery);
        orderRepository.save(order);
        
        return toDeliveryResponse(delivery);
    }

    @Override
    @Transactional
    public DeliveryResponse markDeliverySuccess(String deliveryId, MarkDeliverySuccessRequest request, Principal principal) {
        User shipper = getCurrentShipper(principal);

        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));

        if (delivery.getShipper() == null || !delivery.getShipper().getUserId().equals(shipper.getUserId())) {
            throw new BadRequestException("This delivery is not assigned to current shipper");
        }

        Order order;
        try {
            order = delivery.getOrder();
            if (order == null) throw new NotFoundException("Order not found for delivery");
            order.getId();
        } catch (Exception e) {
            throw new NotFoundException("Order record is missing for this delivery");
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot update completed/cancelled order: " + order.getStatus());
        }

        String currentStatus = normalizeText(delivery.getStatus());
        if (currentStatus == null || !("SHIPPING".equalsIgnoreCase(currentStatus) || "DELAYED".equalsIgnoreCase(currentStatus))) {
            throw new BadRequestException("Delivery must be SHIPPING or DELAYED to mark success");
        }

        LocalDateTime now = LocalDateTime.now();
        delivery.setStatus(DELIVERY_WAITING_STORE_CONFIRM);
        if (delivery.getDeliveredAt() == null) {
            delivery.setDeliveredAt(now);
        }
        delivery.setUpdatedAt(now);

        if (request != null && normalizeText(request.getNotes()) != null) {
            delivery.setNotes(appendNote(delivery.getNotes(), "SHIPPER", shipper.getUsername(), request.getNotes().trim()));
        }

        order.setUpdatedAt(now);
        order.setStatus(OrderStatus.SHIPPING);
        order.setNotes(appendNote(order.getNotes(), "SHIPPER", shipper.getUsername(), "Delivered to store, waiting confirmation"));
        orderRepository.save(order);

        return toDeliveryResponse(deliveryRepository.save(delivery));
    }

    @Override
    public Page<DeliveryResponse> getMyDeliveries(int page, int size, Double lat, Double lon, Principal principal) {
        User shipper = getCurrentShipper(principal);

        if (lat == null || lon == null) {
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "pickedUpAt"));
            return deliveryRepository.findByShipper_UserId(shipper.getUserId(), pageRequest)
                    .map(this::toDeliveryResponse);
        }

        List<Delivery> allDeliveries = deliveryRepository.findByShipper_UserId(shipper.getUserId());
        List<DeliveryResponse> sortedResponses = allDeliveries.stream()
                .map(delivery -> {
                    DeliveryResponse response = toDeliveryResponse(delivery);
                    try {
                        Order order = delivery.getOrder();
                        Store store = order.getStore();
                        if (store != null && store.getLatitude() != null && store.getLongitude() != null) {
                            response.setDistance(geocodingService.getDrivingDistance(lat, lon, store.getLatitude(), store.getLongitude()));
                        } else {
                            log.warn("Cannot calculate distance for delivery {}: Store or Store coordinates are null", delivery.getId());
                        }
                    } catch (Exception e) {
                        log.error("Could not calculate distance for delivery {}: {}", delivery.getId(), e.getMessage());
                    }
                    return response;
                })
                .sorted((r1, r2) -> {
                    if (r1.getDistance() == null && r2.getDistance() == null) return 0;
                    if (r1.getDistance() == null) return 1;
                    if (r2.getDistance() == null) return -1;
                    return r1.getDistance().compareTo(r2.getDistance());
                })
                .collect(Collectors.toList());

        int start = Math.min(page * size, sortedResponses.size());
        int end = Math.min((start + size), sortedResponses.size());
        List<DeliveryResponse> pagedResponses = sortedResponses.subList(start, end);

        return new PageImpl<>(pagedResponses, PageRequest.of(page, size), sortedResponses.size());
    }

    @Override
    public OrderHolderResponse getOrderHolder(String orderId, Principal principal) {
        validateShipper(principal);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        Delivery delivery = deliveryRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new NotFoundException("Delivery not found for order: " + orderId));

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

    @Override
    public DeliveryResponse getDeliveryById(String deliveryId, Principal principal) {
        validateShipper(principal);
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Delivery not found: " + deliveryId));
        return toDeliveryResponse(delivery);
    }

    private void validateShipper(Principal principal) {
        getCurrentShipper(principal);
    }

    private User getCurrentShipper(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.getName()));

        if (user.getRole() == null || !ROLE_SHIPPER.equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalStateException("Only shipper can access this resource");
        }

        return user;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        String orderId = null;
        String storeName = null;
        String storeAddress = null;
        Double storeLat = null;
        Double storeLon = null;

        try {
            Order order = delivery.getOrder();
            if (order != null) {
                orderId = order.getId();
                Store store = order.getStore();
                if (store != null) {
                    storeName = store.getName();
                    storeAddress = store.getAddress();
                    storeLat = store.getLatitude();
                    storeLon = store.getLongitude();
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve Order/Store info for delivery {}: {}", delivery.getId(), e.getMessage());
        }

        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(orderId)
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
                .storeName(storeName)
                .storeAddress(storeAddress)
                .storeLatitude(storeLat)
                .storeLongitude(storeLon)
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}
