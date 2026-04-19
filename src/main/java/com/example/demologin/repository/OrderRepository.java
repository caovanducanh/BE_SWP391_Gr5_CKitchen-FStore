package com.example.demologin.repository;

import com.example.demologin.entity.Order;
import com.example.demologin.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {
    long countByStatusIn(Collection<OrderStatus> statuses);
    long countByStatus(OrderStatus status);
    long countByCreatedAtBetween(LocalDateTime fromDateTime, LocalDateTime toDateTime);
    long countByCreatedAtGreaterThanEqual(LocalDateTime fromDateTime);
    long countByCreatedAtLessThanEqual(LocalDateTime toDateTime);
    long countByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime fromDateTime, LocalDateTime toDateTime);
    long countByStatusAndCreatedAtGreaterThanEqual(OrderStatus status, LocalDateTime fromDateTime);
    long countByStatusAndCreatedAtLessThanEqual(OrderStatus status, LocalDateTime toDateTime);
    long countByStore_Id(String storeId);
    long countByStore_IdAndStatus(String storeId, OrderStatus status);
    long countByStatusAndKitchenIsNull(OrderStatus status);
    long countByKitchen_IdAndStatusIn(String kitchenId, Collection<OrderStatus> statuses);
    long countByKitchen_IdAndStatus(String kitchenId, OrderStatus status);
    long countByKitchen_IdAndStatusInAndRequestedDateBefore(String kitchenId, Collection<OrderStatus> statuses, LocalDate date);
    Page<Order> findByStore_Id(String storeId, Pageable pageable);
    Page<Order> findByStore_IdAndStatus(String storeId, OrderStatus status, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}

