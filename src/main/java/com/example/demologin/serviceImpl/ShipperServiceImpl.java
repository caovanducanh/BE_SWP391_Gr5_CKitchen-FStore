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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipperServiceImpl implements ShipperService {

    private static final String ROLE_SHIPPER = "SHIPPER";
    private static final String DELIVERY_WAITING_STORE_CONFIRM = "WAITING_CONFIRM";

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    public Page<OrderResponse> getAvailableOrders(int page, int size, Principal principal) {
        validateShipper(principal);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "assignedAt"));

        return deliveryRepository.findByOrder_StatusAndShipperIsNull(OrderStatus.PACKED_WAITING_SHIPPER, pageRequest)
                .map(delivery -> {
                    Order order = delivery.getOrder();
                    List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
                    return toOrderResponse(order, items);
                });
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

        Order order = delivery.getOrder();
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

        orderRepository.save(order);
        return toDeliveryResponse(deliveryRepository.save(delivery));
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

        Order order = delivery.getOrder();
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
    public Page<DeliveryResponse> getMyDeliveries(int page, int size, Principal principal) {
        User shipper = getCurrentShipper(principal);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "pickedUpAt"));
        return deliveryRepository.findByShipper_UserId(shipper.getUserId(), pageRequest)
                .map(this::toDeliveryResponse);
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
}
