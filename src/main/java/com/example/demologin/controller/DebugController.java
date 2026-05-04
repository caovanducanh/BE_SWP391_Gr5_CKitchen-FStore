package com.example.demologin.controller;

import com.example.demologin.annotation.PublicEndpoint;
import com.example.demologin.repository.OrderItemBatchRepository;
import com.example.demologin.repository.StoreBatchRepository;
import com.example.demologin.repository.StoreInventoryRepository;
import com.example.demologin.repository.SaleItemRepository;
import com.example.demologin.repository.SalesRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/debug")
public class DebugController {
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final StoreBatchRepository storeBatchRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final SaleItemRepository saleItemRepository;
    private final SalesRecordRepository salesRecordRepository;

    @GetMapping("/inventory-stats")
    public Object getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("orderItemBatchCount", orderItemBatchRepository.count());
        stats.put("storeBatchCount", storeBatchRepository.count());
        stats.put("storeInventoryCount", storeInventoryRepository.count());
        return stats;
    }

    @PostMapping("/clear-inventory")
    @PublicEndpoint
    @Transactional
    public Object clearInventory() {
        // Clear sales records first due to FK constraints if any
        saleItemRepository.deleteAll();
        salesRecordRepository.deleteAll();
        
        // Clear inventory
        storeBatchRepository.deleteAll();
        storeInventoryRepository.deleteAll();
        
        // Clear order-batch links to restart flow
        orderItemBatchRepository.deleteAll();
        
        return Map.of("message", "All store inventory and sales data cleared successfully");
    }

    @GetMapping("/order-batches/{orderId}")
    public Object getOrderBatches(@PathVariable String orderId) {
        return orderItemBatchRepository.findByOrderItem_Order_Id(orderId).stream()
                .map(oib -> Map.of(
                        "orderItemId", oib.getOrderItem().getId(),
                        "productName", oib.getOrderItem().getProduct().getName(),
                        "batchId", oib.getBatch().getId(),
                        "quantity", oib.getQuantity()
                )).toList();
    }
}
