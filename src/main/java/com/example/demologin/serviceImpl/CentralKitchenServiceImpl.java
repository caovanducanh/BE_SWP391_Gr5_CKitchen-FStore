package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.centralkitchen.CreateProductionPlanRequest;
import com.example.demologin.dto.request.centralkitchen.UpdateOrderStatusRequest;
import com.example.demologin.dto.response.*;

import com.example.demologin.enums.ProductCategory;
import com.example.demologin.mapper.ProductMapper;
import org.springframework.data.domain.PageImpl;
import com.example.demologin.entity.Batch;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.IngredientBatch;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.KitchenInventory;
import com.example.demologin.entity.Order;
import com.example.demologin.entity.OrderItem;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.ProductionPlan;
import com.example.demologin.entity.PlanIngredient;
import com.example.demologin.entity.PlanIngredientBatchUsage;
import com.example.demologin.entity.Recipe;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.User;
import com.example.demologin.enums.OrderStatus;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.BatchRepository;
import com.example.demologin.repository.IngredientBatchRepository;
import com.example.demologin.repository.KitchenInventoryRepository;
import com.example.demologin.repository.OrderItemRepository;
import com.example.demologin.repository.OrderRepository;
import com.example.demologin.repository.PlanIngredientRepository;
import com.example.demologin.repository.PlanIngredientBatchUsageRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.ProductionPlanRepository;
import com.example.demologin.repository.RecipeRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.service.CentralKitchenService;
import com.example.demologin.service.IngredientBatchService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    
    private final RecipeRepository recipeRepository;
    private final IngredientBatchRepository ingredientBatchRepository;
    private final PlanIngredientRepository planIngredientRepository;
    private final PlanIngredientBatchUsageRepository planIngredientBatchUsageRepository;
    private final BatchRepository batchRepository;
    private final IngredientBatchService ingredientBatchService;
    private final ProductMapper productMapper;

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
        if (order.getAssignedAt() == null) {
            order.setAssignedAt(LocalDateTime.now());
        }
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
            if (newStatus == OrderStatus.PACKED_WAITING_SHIPPER && currentStatus != OrderStatus.PACKED_WAITING_SHIPPER) {
                handleProductDeductionOnPacking(order, principal);
            }
            
            order.setStatus(newStatus);
            markOrderStatusTimestamp(order, newStatus);
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
    public ProductionPlanResponse getProductionPlanById(String planId, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Production plan not found: " + planId));
                
        if (currentUser.getKitchen() != null && !plan.getKitchen().getId().equals(currentUser.getKitchen().getId())) {
            throw new BadRequestException("Plan does not belong to your kitchen");
        }
        return toProductionPlanResponse(plan);
    }

    @Override
    @Transactional
    public ProductionPlanResponse createProductionPlan(CreateProductionPlanRequest request, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Kitchen kitchen = currentUser.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("You are not assigned to any kitchen");
        }

        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("endDate must be after startDate");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + request.getProductId()));

        List<Recipe> recipes = recipeRepository.findAllByProduct_Id(product.getId());
        if (recipes.isEmpty()) {
            throw new BadRequestException("Product has no recipe defined");
        }

        List<PlanIngredient> planIngredients = new ArrayList<>();
        List<PlanIngredientBatchUsage> usages = new ArrayList<>();
        
        ProductionPlan tempPlan = new ProductionPlan();
        tempPlan.setId(generateProductionPlanId());
        
        for (Recipe recipe : recipes) {
            BigDecimal requiredTotal = recipe.getQuantity().multiply(BigDecimal.valueOf(request.getQuantity()));
            
            List<IngredientBatch> batches = ingredientBatchRepository.findActiveByKitchenAndIngredientOrderByExpiryAsc(
                    kitchen.getId(), recipe.getIngredient().getId());
                    
            BigDecimal remainingToFulfill = requiredTotal;
            for (IngredientBatch batch : batches) {
                if (remainingToFulfill.compareTo(BigDecimal.ZERO) <= 0) break;
                
                BigDecimal qtyToTake = batch.getRemainingQuantity().min(remainingToFulfill);
                remainingToFulfill = remainingToFulfill.subtract(qtyToTake);
                
                usages.add(PlanIngredientBatchUsage.builder()
                        .plan(tempPlan)
                        .ingredientBatch(batch)
                        .quantityUsed(qtyToTake)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            
            if (remainingToFulfill.compareTo(BigDecimal.ZERO) > 0) {
                throw new BadRequestException("Not enough ingredient: " + recipe.getIngredient().getName() + ". Short by: " + remainingToFulfill + " " + recipe.getUnit());
            }
            
            planIngredients.add(PlanIngredient.builder()
                    .plan(tempPlan)
                    .ingredient(recipe.getIngredient())
                    .name(recipe.getIngredient().getName())
                    .quantity(requiredTotal)
                    .qty(requiredTotal)
                    .unit(recipe.getUnit())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        tempPlan.setProduct(product);
        tempPlan.setKitchen(kitchen);
        tempPlan.setQuantity(request.getQuantity());
        tempPlan.setUnit(product.getUnit());
        tempPlan.setStatus("DRAFT");
        tempPlan.setStartDate(request.getStartDate());
        tempPlan.setEndDate(request.getEndDate());
        tempPlan.setStaff(principal.getName());
        tempPlan.setNotes(request.getNotes());
        tempPlan.setCreatedAt(LocalDateTime.now());
        tempPlan.setUpdatedAt(LocalDateTime.now());

        ProductionPlan savedPlan = productionPlanRepository.save(tempPlan);
        
        for (PlanIngredient pi : planIngredients) pi.setPlan(savedPlan);
        for (PlanIngredientBatchUsage usage : usages) usage.setPlan(savedPlan);
        
        planIngredientRepository.saveAll(planIngredients);
        planIngredientBatchUsageRepository.saveAll(usages);

        return toProductionPlanResponse(savedPlan);
    }

    @Override
    @Transactional
    public ProductionPlanResponse startProductionPlan(String planId, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));
                
        if (!"DRAFT".equals(plan.getStatus()) && !"APPROVED".equals(plan.getStatus())) {
            throw new BadRequestException("Can only start DRAFT or APPROVED plans");
        }
        
        List<PlanIngredientBatchUsage> usages = planIngredientBatchUsageRepository.findByPlan_Id(planId);
        for (PlanIngredientBatchUsage usage : usages) {
            IngredientBatch batch = usage.getIngredientBatch();
            BigDecimal newQty = batch.getRemainingQuantity().subtract(usage.getQuantityUsed());
            if (newQty.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Batch " + batch.getBatchNo() + " does not have enough qty now. Please recreate plan.");
            }
            batch.setRemainingQuantity(newQty);
            if (newQty.compareTo(BigDecimal.ZERO) == 0) {
                batch.setStatus("DEPLETED");
            }
            batch.setUpdatedAt(LocalDateTime.now());
            ingredientBatchRepository.save(batch);
            
            KitchenInventory inv = kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id(
                    plan.getKitchen().getId(), batch.getIngredient().getId()).orElse(null);
            if (inv != null) {
                inv.setTotalQuantity(inv.getTotalQuantity().subtract(usage.getQuantityUsed()));
                inv.setUpdatedAt(LocalDateTime.now());
                kitchenInventoryRepository.save(inv);
            }
        }
        
        plan.setStatus("IN_PRODUCTION");
        plan.setUpdatedAt(LocalDateTime.now());
        return toProductionPlanResponse(productionPlanRepository.save(plan));
    }

    @Override
    @Transactional
    public ProductionPlanResponse completeProductionPlan(String planId, String notes, LocalDate expiryDate, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));
                
        if (!"IN_PRODUCTION".equals(plan.getStatus())) {
            throw new BadRequestException("Plan is not in production");
        }
        
        if (expiryDate == null) {
            throw new BadRequestException("expiryDate is required for the finished product batch");
        }
        
        plan.setStatus("COMPLETED");
        if (notes != null && !notes.isBlank()) {
            plan.setNotes(appendInternalNote(plan.getNotes(), principal.getName(), notes));
        }
        plan.setUpdatedAt(LocalDateTime.now());
        ProductionPlan savedPlan = productionPlanRepository.save(plan);
        
        String batchId = "PB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        Batch batch = Batch.builder()
                .id(batchId)
                .plan(savedPlan)
                .product(savedPlan.getProduct())
                .kitchen(savedPlan.getKitchen())
                .quantity(savedPlan.getQuantity())
                .remainingQuantity(savedPlan.getQuantity())
                .unit(savedPlan.getUnit())
                .expiryDate(expiryDate)
                .status("AVAILABLE")
                .staff(principal.getName())
                .notes("Generated from plan " + savedPlan.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        batchRepository.save(batch);
        
        return toProductionPlanResponse(savedPlan);
    }

    @Override
    @Transactional
    public ProductionPlanResponse cancelProductionPlan(String planId, String notes, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));
                
        if ("COMPLETED".equals(plan.getStatus())) {
            throw new BadRequestException("Cannot cancel completed plan");
        }
        
        if ("IN_PRODUCTION".equals(plan.getStatus())) {
            List<PlanIngredientBatchUsage> usages = planIngredientBatchUsageRepository.findByPlan_Id(planId);
            for (PlanIngredientBatchUsage usage : usages) {
                IngredientBatch batch = usage.getIngredientBatch();
                batch.setRemainingQuantity(batch.getRemainingQuantity().add(usage.getQuantityUsed()));
                if ("DEPLETED".equals(batch.getStatus()) && batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    batch.setStatus("ACTIVE");
                }
                batch.setUpdatedAt(LocalDateTime.now());
                ingredientBatchRepository.save(batch);
                
                KitchenInventory inv = kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id(
                        plan.getKitchen().getId(), batch.getIngredient().getId()).orElse(null);
                if (inv != null) {
                    inv.setTotalQuantity(inv.getTotalQuantity().add(usage.getQuantityUsed()));
                    inv.setUpdatedAt(LocalDateTime.now());
                    kitchenInventoryRepository.save(inv);
                }
            }
            planIngredientBatchUsageRepository.deleteAll(usages);
        }
        
        plan.setStatus("CANCELLED");
        if (notes != null && !notes.isBlank()) {
            plan.setNotes(appendInternalNote(plan.getNotes(), principal.getName(), notes));
        }
        plan.setUpdatedAt(LocalDateTime.now());
        return toProductionPlanResponse(productionPlanRepository.save(plan));
    }

    @Override
    public Page<BatchResponse> getProductBatches(String productId, String status, int page, int size, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Kitchen kitchen = currentUser.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Central kitchen staff is not assigned to any kitchen");
        }

        Specification<Batch> spec = Specification.where((root, q, cb) -> cb.equal(root.get("kitchen").get("id"), kitchen.getId()));
        if (productId != null && !productId.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("product").get("id"), productId.trim()));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        }
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return batchRepository.findAll(spec, pageRequest).map(this::toBatchResponse);
    }

    @Override
    public BatchResponse getProductBatchById(String batchId, Principal principal) {
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));
                
        if (currentUser.getKitchen() != null && !batch.getKitchen().getId().equals(currentUser.getKitchen().getId())) {
            throw new BadRequestException("Batch does not belong to your kitchen");
        }
        return toBatchResponse(batch);
    }

    @Override
    public Page<KitchenInventoryDetailResponse> getInventory(String ingredientId, String ingredientName, Boolean lowStock, int page, int size, Principal principal) {
        return ingredientBatchService.getInventory(ingredientId, ingredientName, lowStock, page, size, principal);
    }

    @Override
    public Page<ProductResponse> getProducts(String search, String category, int page, int size, Principal principal) {
        validateCentralKitchenStaff(principal);

        String normalizedSearch = normalizeText(search);
        ProductCategory parsedCategory = parseProductCategory(category);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return productRepository.searchProducts(normalizedSearch, parsedCategory, pageRequest)
                .map(productMapper::toResponse);
    }

    @Override
    public Page<KitchenProductInventoryResponse> getProductInventory(String productId, String productName, int page, int size, Principal principal) {
        validateCentralKitchenStaff(principal);
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Kitchen kitchen = currentUser.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Central kitchen staff is not assigned to any kitchen");
        }

        Specification<Batch> spec = Specification.where((root, query, cb) -> cb.equal(root.get("kitchen").get("id"), kitchen.getId()));
        spec = spec.and((root, query, cb) -> root.get("status").in(List.of("AVAILABLE", "PARTIALLY_DISTRIBUTED")));

        if (productId != null && !productId.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId.trim()));
        }
        if (productName != null && !productName.isBlank()) {
            String keyword = "%" + productName.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("product").get("name")), keyword));
        }

        List<Batch> activeBatches = batchRepository.findAll(spec);

        Map<Product, List<Batch>> grouped = activeBatches.stream()
                .collect(Collectors.groupingBy(Batch::getProduct));

        List<KitchenProductInventoryResponse> results = grouped.entrySet().stream()
                .map(entry -> {
                    Product p = entry.getKey();
                    List<Batch> batches = entry.getValue();
                    int total = batches.stream().mapToInt(Batch::getRemainingQuantity).sum();

                    return KitchenProductInventoryResponse.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .totalRemainingQuantity(total)
                            .unit(p.getUnit())
                            .batches(batches.stream().map(b -> KitchenProductInventoryResponse.ProductBatchDetailResponse.builder()
                                    .batchId(b.getId())
                                    .remainingQuantity(b.getRemainingQuantity())
                                    .expiryDate(b.getExpiryDate())
                                    .status(b.getStatus())
                                    .build()).toList())
                            .build();
                })
                .sorted(Comparator.comparing(KitchenProductInventoryResponse::getProductName))
                .toList();

        int start = Math.min((int) PageRequest.of(page, size).getOffset(), results.size());
        int end = Math.min((start + size), results.size());
        List<KitchenProductInventoryResponse> paginatedResults = results.subList(start, end);

        return new PageImpl<>(paginatedResults, PageRequest.of(page, size), results.size());
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

    private ProductCategory parseProductCategory(String category) {
        String value = normalizeText(category);
        if (value == null) {
            return null;
        }
        try {
            return ProductCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid product category: " + category);
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

    private void markOrderStatusTimestamp(Order order, OrderStatus status) {
        LocalDateTime now = LocalDateTime.now();
        switch (status) {
            case ASSIGNED -> {
                if (order.getAssignedAt() == null) order.setAssignedAt(now);
            }
            case IN_PROGRESS, PROCESSING -> {
                if (order.getAssignedAt() == null) order.setAssignedAt(now);
                if (order.getInProgressAt() == null) order.setInProgressAt(now);
            }
            case PACKED_WAITING_SHIPPER -> {
                if (order.getAssignedAt() == null) order.setAssignedAt(now);
                if (order.getInProgressAt() == null) order.setInProgressAt(now);
                if (order.getPackedWaitingShipperAt() == null) order.setPackedWaitingShipperAt(now);
            }
            case SHIPPING -> {
                if (order.getAssignedAt() == null) order.setAssignedAt(now);
                if (order.getInProgressAt() == null) order.setInProgressAt(now);
                if (order.getPackedWaitingShipperAt() == null) order.setPackedWaitingShipperAt(now);
                if (order.getShippingAt() == null) order.setShippingAt(now);
            }
            case DELIVERED -> {
                if (order.getAssignedAt() == null) order.setAssignedAt(now);
                if (order.getInProgressAt() == null) order.setInProgressAt(now);
                if (order.getPackedWaitingShipperAt() == null) order.setPackedWaitingShipperAt(now);
                if (order.getShippingAt() == null) order.setShippingAt(now);
                if (order.getDeliveredAt() == null) order.setDeliveredAt(now);
            }
            case CANCELLED -> {
                if (order.getCancelledAt() == null) order.setCancelledAt(now);
            }
            default -> {
                // do nothing
            }
        }
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
        
        List<PlanIngredientResponse> ingredientResponses = null;
        if (plan.getId() != null) {
            List<PlanIngredient> pis = planIngredientRepository.findByPlan_Id(plan.getId());
            if (pis != null && !pis.isEmpty()) {
                ingredientResponses = pis.stream().map(pi -> {
                    BigDecimal available = ingredientBatchRepository.sumRemainingByKitchenAndIngredient(
                            plan.getKitchen().getId(), pi.getIngredient().getId());
                    return PlanIngredientResponse.builder()
                            .ingredientId(pi.getIngredient().getId())
                            .ingredientName(pi.getIngredient().getName())
                            .requiredQuantity(pi.getQuantity())
                            .availableQuantity(available != null ? available : BigDecimal.ZERO)
                            .unit(pi.getUnit())
                            .sufficient(available != null && available.compareTo(pi.getQuantity()) >= 0)
                            .build();
                }).toList();
            }
        }

        return ProductionPlanResponse.builder()
                .id(plan.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .kitchenId(plan.getKitchen() != null ? plan.getKitchen().getId() : null)
                .kitchenName(plan.getKitchen() != null ? plan.getKitchen().getName() : null)
                .quantity(plan.getQuantity())
                .unit(plan.getUnit())
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .staff(plan.getStaff())
                .notes(plan.getNotes())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .ingredients(ingredientResponses)
                .build();
    }
    
    private BatchResponse toBatchResponse(Batch batch) {
        List<BatchIngredientUsageResponse> usages = null;
        if (batch.getPlan() != null) {
            List<PlanIngredientBatchUsage> pu = planIngredientBatchUsageRepository.findByPlan_Id(batch.getPlan().getId());
            usages = pu.stream().map(u -> BatchIngredientUsageResponse.builder()
                    .ingredientBatchId(u.getIngredientBatch().getId())
                    .ingredientId(u.getIngredientBatch().getIngredient().getId())
                    .ingredientName(u.getIngredientBatch().getIngredient().getName())
                    .batchNo(u.getIngredientBatch().getBatchNo())
                    .quantityUsed(u.getQuantityUsed())
                    .unit(u.getIngredientBatch().getUnit())
                    .expiryDate(u.getIngredientBatch().getExpiryDate())
                    .build()).toList();
        }

        return BatchResponse.builder()
                .id(batch.getId())
                .planId(batch.getPlan() != null ? batch.getPlan().getId() : null)
                .productId(batch.getProduct() != null ? batch.getProduct().getId() : null)
                .productName(batch.getProduct() != null ? batch.getProduct().getName() : null)
                .kitchenId(batch.getKitchen() != null ? batch.getKitchen().getId() : null)
                .kitchenName(batch.getKitchen() != null ? batch.getKitchen().getName() : null)
                .quantity(batch.getQuantity())
                .remainingQuantity(batch.getRemainingQuantity())
                .unit(batch.getUnit())
                .expiryDate(batch.getExpiryDate())
                .status(batch.getStatus())
                .staff(batch.getStaff())
                .notes(batch.getNotes())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .ingredientBatchUsages(usages)
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
    @Override
    public RecipeCheckResponse checkRecipeAvailability(String productId, Integer quantity, Principal principal) {
        validateCentralKitchenStaff(principal);
        User currentUser = getCurrentCentralKitchenStaff(principal);
        Kitchen kitchen = currentUser.getKitchen();
        if (kitchen == null) {
            throw new BadRequestException("Central kitchen staff is not assigned to any kitchen");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));

        List<Recipe> recipes = recipeRepository.findAllByProduct_Id(productId);
        if (recipes.isEmpty()) {
            throw new BadRequestException("Recipe not defined for product: " + product.getName());
        }

        List<RecipeCheckResponse.IngredientCheckDetail> details = new ArrayList<>();
        boolean canProduce = true;

        for (Recipe recipe : recipes) {
            Ingredient ingredient = recipe.getIngredient();
            BigDecimal requiredQty = recipe.getQuantity().multiply(new BigDecimal(quantity));
            
            BigDecimal availableQty = kitchenInventoryRepository.findByKitchen_IdAndIngredient_Id(kitchen.getId(), ingredient.getId())
                    .map(KitchenInventory::getTotalQuantity)
                    .orElse(BigDecimal.ZERO);

            boolean isSufficient = availableQty.compareTo(requiredQty) >= 0;
            BigDecimal shortage = isSufficient ? BigDecimal.ZERO : requiredQty.subtract(availableQty);

            if (!isSufficient) {
                canProduce = false;
            }

            details.add(RecipeCheckResponse.IngredientCheckDetail.builder()
                    .ingredientId(ingredient.getId())
                    .ingredientName(ingredient.getName())
                    .requiredQuantity(requiredQty)
                    .availableQuantity(availableQty)
                    .unit(recipe.getUnit())
                    .isSufficient(isSufficient)
                    .shortage(shortage)
                    .build());
        }

        return RecipeCheckResponse.builder()
                .productId(productId)
                .productName(product.getName())
                .requestedQuantity(quantity)
                .ingredients(details)
                .canProduce(canProduce)
                .build();
    }

    @Override
    @Transactional
    public BatchResponse updateBatch(String batchId, com.example.demologin.dto.request.centralkitchen.UpdateBatchRequest request, Principal principal) {
        validateCentralKitchenStaff(principal);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Batch not found: " + batchId));

        if (request.getExpiryDate() != null) {
            batch.setExpiryDate(request.getExpiryDate());
        }
        if (request.getStatus() != null) {
            batch.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            batch.setNotes(request.getNotes());
        }
        batch.setUpdatedAt(LocalDateTime.now());

        return toBatchResponse(batchRepository.save(batch));
    }

    private void handleProductDeductionOnPacking(Order order, Principal principal) {
        Kitchen kitchen = getCurrentCentralKitchenStaff(principal).getKitchen();
        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());

        for (OrderItem item : items) {
            int quantityToDeduct = item.getQuantity();
            String productId = item.getProduct().getId();

            // Find all active batches for this product in this kitchen, sorted by expiry date (FEFO)
            Specification<Batch> spec = Specification.where((root, query, cb) -> cb.equal(root.get("kitchen").get("id"), kitchen.getId()));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
            spec = spec.and((root, query, cb) -> root.get("status").in(List.of("AVAILABLE", "PARTIALLY_DISTRIBUTED")));

            List<Batch> batches = batchRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "expiryDate"));

            int totalAvailable = batches.stream().mapToInt(Batch::getRemainingQuantity).sum();
            if (totalAvailable < quantityToDeduct) {
                throw new BadRequestException("Not enough product in inventory: " + item.getProduct().getName() + 
                    ". Required: " + quantityToDeduct + ", Available: " + totalAvailable);
            }

            // Deduct from batches using FEFO
            int remainingToDeduct = quantityToDeduct;
            for (Batch batch : batches) {
                if (remainingToDeduct <= 0) break;

                int batchRemaining = batch.getRemainingQuantity();
                if (batchRemaining <= remainingToDeduct) {
                    remainingToDeduct -= batchRemaining;
                    batch.setRemainingQuantity(0);
                    batch.setStatus("DISTRIBUTED");
                } else {
                    batch.setRemainingQuantity(batchRemaining - remainingToDeduct);
                    remainingToDeduct = 0;
                    batch.setStatus("PARTIALLY_DISTRIBUTED");
                }
                batch.setUpdatedAt(LocalDateTime.now());
                batchRepository.save(batch);
            }
        }
    }
}
