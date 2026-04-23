package com.example.demologin.repository;

import com.example.demologin.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {
    Optional<Delivery> findByOrder_Id(String orderId);
    Optional<Delivery> findByPickupQrCode(String pickupQrCode);
    Page<Delivery> findByOrder_Store_Id(String storeId, Pageable pageable);
    Page<Delivery> findByOrder_Store_IdAndStatus(String storeId, String status, Pageable pageable);
    Page<Delivery> findByCoordinator_UserId(Long coordinatorId, Pageable pageable);
    Page<Delivery> findByCoordinator_UserIdAndStatus(Long coordinatorId, String status, Pageable pageable);
    Page<Delivery> findByShipper_UserId(Long shipperId, Pageable pageable);
    List<Delivery> findByShipper_UserId(Long shipperId);
    Page<Delivery> findByOrder_StatusAndShipperIsNull(com.example.demologin.enums.OrderStatus orderStatus, Pageable pageable);
    List<Delivery> findByOrder_StatusAndShipperIsNull(com.example.demologin.enums.OrderStatus orderStatus);
    List<Delivery> findByOrder_IdIn(List<String> orderIds);
    long countByCoordinator_UserIdAndStatusIn(Long coordinatorId, Collection<String> statuses);
    long countByOrder_Store_IdAndStatusIn(String storeId, Collection<String> statuses);
}
