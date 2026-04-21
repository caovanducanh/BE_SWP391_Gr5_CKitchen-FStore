package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.DailyRevenueByStoreResponse;
import com.example.demologin.dto.response.DailyRevenueRangeResponse;
import com.example.demologin.dto.response.DailyRevenueReportResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.SalesReportClearResponse;
import com.example.demologin.dto.response.SalesReportImportResponse;
import com.example.demologin.dto.response.StoreDailySalesDetailItemResponse;
import com.example.demologin.dto.response.StoreDailySalesDetailResponse;
import com.example.demologin.dto.response.StoreDailySalesResponse;
import com.example.demologin.dto.response.StoreRevenueItemResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.dto.response.StoreTotalRevenueResponse;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.SaleItem;
import com.example.demologin.entity.SalesRecord;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.StoreInventory;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.BusinessException;
import com.example.demologin.exception.exceptions.ConflictException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.SaleItemRepository;
import com.example.demologin.repository.SalesRecordRepository;
import com.example.demologin.repository.StoreInventoryRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesReportServiceImpl implements SalesReportService {

    private static final Pattern SALES_ID_PATTERN = Pattern.compile("^SR(\\d+)$");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SalesRecordRepository salesRecordRepository;
    private final SaleItemRepository saleItemRepository;
    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreRepository storeRepository;
    private final KitchenRepository kitchenRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public byte[] exportTemplate(java.security.Principal principal) {
        User currentUser = getCurrentStoreStaff(principal);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet reportSheet = workbook.createSheet("sales_report");
            Row header = reportSheet.createRow(0);
            header.createCell(0).setCellValue("product_id");
            header.createCell(1).setCellValue("quantity");
            header.createCell(2).setCellValue("unit");
            header.createCell(3).setCellValue("unit_price");

            Row sample = reportSheet.createRow(1);
            sample.createCell(0).setCellValue("PROD001");
            sample.createCell(1).setCellValue(1);
            sample.createCell(2).setCellValue("pcs");
            sample.createCell(3).setCellValue(25000);

            Sheet noteSheet = workbook.createSheet("notes");
            noteSheet.createRow(0).createCell(0).setCellValue("Rules");
            noteSheet.createRow(1).createCell(0).setCellValue("1) Store is taken from authenticated store staff account, no store_id column needed.");
            noteSheet.createRow(2).createCell(0).setCellValue("2) Report date is taken from API date param, no sale_date column needed.");
            noteSheet.createRow(3).createCell(0).setCellValue("3) quantity must be positive integer and cannot exceed current stock.");
            noteSheet.createRow(4).createCell(0).setCellValue("4) unit_price must be >= 0.");
            noteSheet.createRow(5).createCell(0).setCellValue("5) product_id must exist in product catalog.");

            for (int i = 0; i <= 3; i++) {
                reportSheet.autoSizeColumn(i);
            }
            noteSheet.autoSizeColumn(0);

            return writeWorkbookToBytes(workbook, out);
        } catch (IOException ex) {
            throw new BusinessException("Cannot generate sales report template");
        }
    }

    @Override
    @Transactional
    public SalesReportImportResponse importSalesReport(MultipartFile file, LocalDate reportDate, java.security.Principal principal) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Excel file is required");
        }

        if (reportDate == null) {
            throw new BadRequestException("Report date is required");
        }

        User currentUser = getCurrentStoreStaff(principal);
        String storeId = currentUser.getStore().getId();

        if (salesRecordRepository.existsByStoreIdAndDate(storeId, reportDate)) {
            throw new ConflictException("Sales report for this date already exists. Please clear it before re-importing.");
        }

        List<ParsedSaleRow> parsedRows = parseExcelRows(file);
        if (parsedRows.isEmpty()) {
            throw new BadRequestException("No valid sale item rows found in Excel file");
        }

        Set<String> productIds = parsedRows.stream().map(ParsedSaleRow::productId).collect(Collectors.toSet());
        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<String> missingProducts = productIds.stream()
                .filter(id -> !productMap.containsKey(id))
                .sorted()
                .toList();
        if (!missingProducts.isEmpty()) {
            throw new BadRequestException("Unknown product_id: " + String.join(", ", missingProducts));
        }

        List<StoreInventory> inventories = storeInventoryRepository.findByStoreIdAndProductIdIn(storeId, productIds);
        Map<String, StoreInventory> inventoryByProduct = inventories.stream()
                .collect(Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));

        List<String> missingInventory = productIds.stream()
                .filter(id -> !inventoryByProduct.containsKey(id))
                .sorted()
                .toList();
        if (!missingInventory.isEmpty()) {
            throw new BadRequestException("Store inventory not found for product_id: " + String.join(", ", missingInventory));
        }

        Map<String, Integer> soldQuantityByProduct = new HashMap<>();
        for (ParsedSaleRow row : parsedRows) {
            soldQuantityByProduct.merge(row.productId(), row.quantity(), Integer::sum);
        }

        List<String> stockErrors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : soldQuantityByProduct.entrySet()) {
            String productId = entry.getKey();
            int soldQty = entry.getValue();
            StoreInventory inventory = inventoryByProduct.get(productId);
            if (inventory.getQuantity() < soldQty) {
                stockErrors.add("product_id " + productId + " sold " + soldQty + " but stock is only " + inventory.getQuantity());
            }
        }

        for (ParsedSaleRow row : parsedRows) {
            StoreInventory inventory = inventoryByProduct.get(row.productId());
            if (!inventory.getUnit().equalsIgnoreCase(row.unit())) {
                stockErrors.add("product_id " + row.productId() + " has unit '" + row.unit() + "' but inventory unit is '" + inventory.getUnit() + "'");
            }
        }

        if (!stockErrors.isEmpty()) {
            throw new BadRequestException("Invalid sales report data: " + String.join("; ", stockErrors));
        }

        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        String reportId = generateSalesRecordId();

        BigDecimal totalRevenue = parsedRows.stream()
                .map(r -> r.unitPrice().multiply(BigDecimal.valueOf(r.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SalesRecord salesRecord = SalesRecord.builder()
                .id(reportId)
                .store(currentUser.getStore())
                .date(reportDate)
                .totalRevenue(totalRevenue)
                .recordedBy(currentUser.getUsername())
                .recordedAt(now)
                .createdAt(now)
                .build();
        salesRecordRepository.save(salesRecord);

        List<SaleItem> items = parsedRows.stream()
                .map(row -> SaleItem.builder()
                        .sale(salesRecord)
                        .product(productMap.get(row.productId()))
                        .quantity(row.quantity())
                        .unit(row.unit())
                        .unitPrice(row.unitPrice())
                        .createdAt(now)
                        .build())
                .toList();
        saleItemRepository.saveAll(items);

        for (Map.Entry<String, Integer> entry : soldQuantityByProduct.entrySet()) {
            StoreInventory inventory = inventoryByProduct.get(entry.getKey());
            inventory.setQuantity(inventory.getQuantity() - entry.getValue());
            inventory.setUpdatedAt(now);
        }
        storeInventoryRepository.saveAll(inventoryByProduct.values());

        int totalQuantity = parsedRows.stream().mapToInt(ParsedSaleRow::quantity).sum();

        return SalesReportImportResponse.builder()
                .reportId(reportId)
                .storeId(storeId)
                .reportDate(reportDate)
                .itemCount(parsedRows.size())
                .totalQuantity(totalQuantity)
                .totalRevenue(totalRevenue)
                .importedBy(currentUser.getUsername())
                .importedAt(now)
                .build();
    }

    @Override
    @Transactional
    public SalesReportClearResponse clearSalesReport(LocalDate reportDate, java.security.Principal principal) {
        if (reportDate == null) {
            throw new BadRequestException("Report date is required");
        }

        User currentUser = getCurrentStoreStaff(principal);
        String storeId = currentUser.getStore().getId();

        SalesRecord record = salesRecordRepository.findByStoreIdAndDate(storeId, reportDate)
                .orElseThrow(() -> new NotFoundException("No sales report found for this date"));

        List<SaleItem> saleItems = saleItemRepository.findBySaleId(record.getId());
        Map<String, Integer> restoreQtyByProduct = new HashMap<>();
        for (SaleItem item : saleItems) {
            restoreQtyByProduct.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
        }

        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        if (!restoreQtyByProduct.isEmpty()) {
            Set<String> productIds = restoreQtyByProduct.keySet();
            List<StoreInventory> existingInventories = storeInventoryRepository.findByStoreIdAndProductIdIn(storeId, productIds);
            Map<String, StoreInventory> inventoryByProduct = existingInventories.stream()
                    .collect(Collectors.toMap(inv -> inv.getProduct().getId(), inv -> inv));

            Map<String, Product> products = productRepository.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            for (Map.Entry<String, Integer> entry : restoreQtyByProduct.entrySet()) {
                String productId = entry.getKey();
                int qty = entry.getValue();

                StoreInventory inventory = inventoryByProduct.get(productId);
                if (inventory == null) {
                    Product product = products.get(productId);
                    if (product == null) {
                        continue;
                    }
                    inventory = StoreInventory.builder()
                            .store(currentUser.getStore())
                            .product(product)
                            .quantity(qty)
                            .unit(product.getUnit())
                            .minStock(0)
                            .updatedAt(now)
                            .build();
                } else {
                    inventory.setQuantity(inventory.getQuantity() + qty);
                    inventory.setUpdatedAt(now);
                }
                inventoryByProduct.put(productId, inventory);
            }
            storeInventoryRepository.saveAll(inventoryByProduct.values());
        }

        saleItemRepository.deleteBySaleId(record.getId());
        salesRecordRepository.delete(record);

        int restoredQuantity = restoreQtyByProduct.values().stream().mapToInt(Integer::intValue).sum();

        return SalesReportClearResponse.builder()
                .reportId(record.getId())
                .storeId(storeId)
                .reportDate(reportDate)
                .restoredItems(saleItems.size())
                .restoredQuantity(restoredQuantity)
                .clearedAt(now)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StoreDailySalesResponse> getMyDailySales(LocalDate fromDate, LocalDate toDate, java.security.Principal principal, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (size <= 0) {
            throw new BadRequestException("size must be > 0");
        }

        User currentUser = getCurrentStoreStaff(principal);
        Page<SalesRecord> records = salesRecordRepository.findByStoreIdAndDateRange(
                currentUser.getStore().getId(),
                fromDate,
                toDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"))
        );

        return records.map(record -> StoreDailySalesResponse.builder()
                .reportId(record.getId())
                .reportDate(record.getDate())
                .totalRevenue(nonNull(record.getTotalRevenue()))
                .itemCount((int) saleItemRepository.countBySaleId(record.getId()))
                .totalQuantity(nonNullInteger(saleItemRepository.sumQuantityBySaleId(record.getId())))
                .recordedBy(record.getRecordedBy())
                .recordedAt(record.getRecordedAt())
                .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StoreDailySalesDetailResponse getMyDailySalesDetail(LocalDate reportDate, java.security.Principal principal, int page, int size) {
            if (reportDate == null) {
                throw new BadRequestException("Report date is required");
            }
                if (page < 0) {
                    throw new BadRequestException("page must be >= 0");
                }
                if (size <= 0) {
                    throw new BadRequestException("size must be > 0");
                }

            User currentUser = getCurrentStoreStaff(principal);
            SalesRecord record = salesRecordRepository.findByStoreIdAndDate(currentUser.getStore().getId(), reportDate)
                .orElseThrow(() -> new NotFoundException("No sales report found for this date"));

                Page<SaleItem> saleItemPage = saleItemRepository.findBySaleId(
                        record.getId(),
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"))
                );
                List<StoreDailySalesDetailItemResponse> items = saleItemPage.getContent().stream()
                .map(item -> StoreDailySalesDetailItemResponse.builder()
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unit(item.getUnit())
                    .unitPrice(item.getUnitPrice())
                    .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build())
                .toList();

            int totalQuantity = nonNullInteger(saleItemRepository.sumQuantityBySaleId(record.getId()));

            return StoreDailySalesDetailResponse.builder()
                .reportId(record.getId())
                .storeId(record.getStore().getId())
                .storeName(record.getStore().getName())
                .reportDate(record.getDate())
                .totalRevenue(nonNull(record.getTotalRevenue()))
                .itemCount((int) saleItemPage.getTotalElements())
                .totalQuantity(totalQuantity)
                .recordedBy(record.getRecordedBy())
                .recordedAt(record.getRecordedAt())
                .page(saleItemPage.getNumber())
                .size(saleItemPage.getSize())
                .totalItems(saleItemPage.getTotalElements())
                .totalPages(saleItemPage.getTotalPages())
                .hasNext(saleItemPage.hasNext())
                .items(items)
                .build();
            }

    @Override
    @Transactional(readOnly = true)
        public DailyRevenueRangeResponse getDailyRevenue(LocalDate fromDate, LocalDate toDate, String storeId) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        String normalizedStoreId = storeId == null ? null : storeId.trim();
        if (normalizedStoreId != null && normalizedStoreId.isEmpty()) {
            normalizedStoreId = null;
        }

        List<SalesRecordRepository.DailyRevenueProjection> projections =
            salesRecordRepository.summarizeDailyRevenue(fromDate, toDate, normalizedStoreId);

        Map<LocalDate, List<SalesRecordRepository.DailyRevenueProjection>> groupedByDate = new LinkedHashMap<>();
        for (SalesRecordRepository.DailyRevenueProjection projection : projections) {
            groupedByDate.computeIfAbsent(projection.getReportDate(), ignored -> new ArrayList<>()).add(projection);
        }

        List<DailyRevenueReportResponse> dailyReports = groupedByDate.entrySet().stream()
            .map(entry -> {
                List<DailyRevenueByStoreResponse> stores = entry.getValue().stream()
                    .map(p -> DailyRevenueByStoreResponse.builder()
                        .storeId(p.getStoreId())
                        .storeName(p.getStoreName())
                        .totalRevenue(nonNull(p.getTotalRevenue()))
                        .reportCount(p.getReportCount())
                        .build())
                    .toList();

                BigDecimal dailyTotalRevenue = stores.stream()
                    .map(DailyRevenueByStoreResponse::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                return DailyRevenueReportResponse.builder()
                    .reportDate(entry.getKey())
                    .totalRevenue(dailyTotalRevenue)
                    .storeCount(stores.size())
                    .stores(stores)
                    .build();
            })
            .toList();

        BigDecimal totalRevenue = dailyReports.stream()
            .map(DailyRevenueReportResponse::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailyRevenueRangeResponse.builder()
            .fromDate(fromDate)
            .toDate(toDate)
                .totalRevenue(totalRevenue)
            .dayCount(dailyReports.size())
            .days(dailyReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreTotalRevenueResponse getStoreTotalRevenue(LocalDate fromDate, LocalDate toDate, String storeId) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate");
        }

        String normalizedStoreId = storeId == null ? null : storeId.trim();
        if (normalizedStoreId != null && normalizedStoreId.isEmpty()) {
            normalizedStoreId = null;
        }

        if (normalizedStoreId == null) {
            List<StoreRevenueItemResponse> stores = salesRecordRepository.summarizeRevenueByStore(fromDate, toDate)
                .stream()
                .map(p -> StoreRevenueItemResponse.builder()
                    .storeId(p.getStoreId())
                    .storeName(p.getStoreName())
                    .totalReportRevenue(nonNull(p.getTotalRevenue()))
                    .build())
                .toList();

            BigDecimal totalReportRevenue = stores.stream()
                .map(StoreRevenueItemResponse::getTotalReportRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return StoreTotalRevenueResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalReportRevenue(totalReportRevenue)
                .storeCount(stores.size())
                .stores(stores)
                .build();
        }

        BigDecimal totalReportRevenue = nonNull(
                salesRecordRepository.sumRevenueByStoreAndDateRange(normalizedStoreId, fromDate, toDate)
        );

        String storeName = storeRepository.findById(normalizedStoreId)
            .map(Store::getName)
            .orElse(null);

        return StoreTotalRevenueResponse.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .totalReportRevenue(totalReportRevenue)
            .storeCount(1)
            .stores(List.of(
                StoreRevenueItemResponse.builder()
                    .storeId(normalizedStoreId)
                    .storeName(storeName)
                    .totalReportRevenue(totalReportRevenue)
                    .build()
            ))
                .build();
    }

            @Override
            @Transactional(readOnly = true)
            public List<StoreResponse> getAllStoresForRevenueFilter() {
            return storeRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(store -> StoreResponse.builder()
                    .id(store.getId())
                    .name(store.getName())
                    .address(store.getAddress())
                    .phone(store.getPhone())
                    .manager(store.getManager())
                    .status(store.getStatus())
                    .openDate(store.getOpenDate())
                    .createdAt(store.getCreatedAt())
                    .updatedAt(store.getUpdatedAt())
                    .build())
                .toList();
            }

            @Override
            @Transactional(readOnly = true)
            public List<KitchenResponse> getAllKitchensForRevenueFilter() {
            return kitchenRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(kitchen -> KitchenResponse.builder()
                    .id(kitchen.getId())
                    .name(kitchen.getName())
                    .address(kitchen.getAddress())
                    .phone(kitchen.getPhone())
                    .capacity(kitchen.getCapacity())
                    .status(kitchen.getStatus())
                    .createdAt(kitchen.getCreatedAt())
                    .updatedAt(kitchen.getUpdatedAt())
                    .build())
                .toList();
            }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDailyRevenueReport(LocalDate fromDate, LocalDate toDate, String storeId) {
        DailyRevenueRangeResponse report = getDailyRevenue(fromDate, toDate, storeId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet summarySheet = workbook.createSheet("summary");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

            Row row0 = summarySheet.createRow(0);
            row0.createCell(0).setCellValue("Manager Daily Revenue Report");

            Row row1 = summarySheet.createRow(1);
            row1.createCell(0).setCellValue("from_date");
            row1.createCell(1).setCellValue(report.getFromDate() == null ? "" : report.getFromDate().format(dateFormatter));

            Row row2 = summarySheet.createRow(2);
            row2.createCell(0).setCellValue("to_date");
            row2.createCell(1).setCellValue(report.getToDate() == null ? "" : report.getToDate().format(dateFormatter));

            Row row3 = summarySheet.createRow(3);
            row3.createCell(0).setCellValue("store_filter");
            row3.createCell(1).setCellValue(storeId == null ? "" : storeId.trim());

            Row row4 = summarySheet.createRow(4);
            row4.createCell(0).setCellValue("day_count");
            row4.createCell(1).setCellValue(report.getDayCount());

            Row row5 = summarySheet.createRow(5);
            row5.createCell(0).setCellValue("total_revenue");
            row5.createCell(1).setCellValue(nonNull(report.getTotalRevenue()).doubleValue());

            Map<String, StoreRevenueAggregate> revenueByStore = new LinkedHashMap<>();
            for (DailyRevenueReportResponse day : report.getDays()) {
                for (DailyRevenueByStoreResponse store : day.getStores()) {
                    String key = (store.getStoreId() == null || store.getStoreId().isBlank())
                            ? "UNKNOWN_STORE"
                            : store.getStoreId();
                    StoreRevenueAggregate aggregate = revenueByStore.computeIfAbsent(
                            key,
                            ignored -> new StoreRevenueAggregate(store.getStoreId(), store.getStoreName(), BigDecimal.ZERO)
                    );
                    aggregate.totalRevenue = aggregate.totalRevenue.add(nonNull(store.getTotalRevenue()));
                }
            }

            int storeSectionStartRow = 7;
            Row storeSectionTitle = summarySheet.createRow(storeSectionStartRow);
            storeSectionTitle.createCell(0).setCellValue("Store Revenue Breakdown");

            Row storeHeaderRow = summarySheet.createRow(storeSectionStartRow + 1);
            Row storeRevenueRow = summarySheet.createRow(storeSectionStartRow + 2);

            storeHeaderRow.createCell(0).setCellValue("store");
            storeRevenueRow.createCell(0).setCellValue("revenue");

            int storeColumnIndex = 1;
            for (StoreRevenueAggregate aggregate : revenueByStore.values()) {
                String storeLabel;
                if (aggregate.storeId == null || aggregate.storeId.isBlank()) {
                    storeLabel = aggregate.storeName == null ? "UNKNOWN_STORE" : aggregate.storeName;
                } else {
                    storeLabel = aggregate.storeName == null || aggregate.storeName.isBlank()
                            ? aggregate.storeId
                            : aggregate.storeId + " - " + aggregate.storeName;
                }

                storeHeaderRow.createCell(storeColumnIndex).setCellValue(storeLabel);
                storeRevenueRow.createCell(storeColumnIndex).setCellValue(nonNull(aggregate.totalRevenue).doubleValue());
                storeColumnIndex++;
            }

            Sheet detailsSheet = workbook.createSheet("daily_details");
            Row header = detailsSheet.createRow(0);
            header.createCell(0).setCellValue("report_date");
            header.createCell(1).setCellValue("store_id");
            header.createCell(2).setCellValue("store_name");
            header.createCell(3).setCellValue("store_total_revenue");

            int rowIndex = 1;
            for (DailyRevenueReportResponse day : report.getDays()) {
                for (DailyRevenueByStoreResponse store : day.getStores()) {
                    Row row = detailsSheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(day.getReportDate() == null ? "" : day.getReportDate().format(dateFormatter));
                    row.createCell(1).setCellValue(store.getStoreId() == null ? "" : store.getStoreId());
                    row.createCell(2).setCellValue(store.getStoreName() == null ? "" : store.getStoreName());
                    row.createCell(3).setCellValue(nonNull(store.getTotalRevenue()).doubleValue());
                }
            }

            int summaryAutoSizeMaxColumn = Math.max(3, revenueByStore.size());
            for (int i = 0; i <= summaryAutoSizeMaxColumn; i++) {
                summarySheet.autoSizeColumn(i);
            }
            for (int i = 0; i <= 3; i++) {
                detailsSheet.autoSizeColumn(i);
            }

            return writeWorkbookToBytes(workbook, out);
        } catch (IOException ex) {
            throw new BusinessException("Cannot export manager revenue report");
        }
    }

    byte[] writeWorkbookToBytes(Workbook workbook, ByteArrayOutputStream out) throws IOException {
        workbook.write(out);
        return out.toByteArray();
    }

    private List<ParsedSaleRow> parseExcelRows(MultipartFile file) {
        DataFormatter formatter = new DataFormatter(Locale.US);
        List<ParsedSaleRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BadRequestException("Excel file does not contain any sheet");
            }

            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                String prefix = "Row " + (i + 1) + ": ";
                String productId = readStringCell(row.getCell(0), formatter);
                if (productId.isBlank()) {
                    errors.add(prefix + "product_id is required");
                    continue;
                }

                Integer quantity = readIntegerCell(row.getCell(1), formatter);
                if (quantity == null || quantity <= 0) {
                    errors.add(prefix + "quantity must be a positive integer");
                    continue;
                }

                String unit = readStringCell(row.getCell(2), formatter);
                if (unit.isBlank()) {
                    errors.add(prefix + "unit is required");
                    continue;
                }

                BigDecimal unitPrice = readDecimalCell(row.getCell(3), formatter);
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(prefix + "unit_price must be >= 0");
                    continue;
                }

                rows.add(new ParsedSaleRow(productId, quantity, unit, unitPrice));
            }
        } catch (IOException ex) {
            throw new BusinessException("Cannot read uploaded Excel file");
        } catch (Exception ex) {
            throw new BadRequestException("Invalid Excel format. Please upload a valid .xlsx file");
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors));
        }

        return rows;
    }

    private String readStringCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Integer readIntegerCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (value % 1 != 0) {
                return null;
            }
            return (int) value;
        }

        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal readDecimalCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }

        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate readDateCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZONE_ID).toLocalDate();
        }

        String text = formatter.formatCellValue(cell).trim();
        if (text.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (int i = 0; i <= 3; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && !new DataFormatter().formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String generateSalesRecordId() {
        int maxSequence = salesRecordRepository.findAllIds().stream()
                .map(SalesReportServiceImpl::extractSequence)
                .max(Integer::compareTo)
                .orElse(0);
        return String.format("SR%08d", maxSequence + 1);
    }

    private static int extractSequence(String id) {
        if (id == null) {
            return 0;
        }

        Matcher matcher = SALES_ID_PATTERN.matcher(id);
        if (!matcher.matches()) {
            return 0;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private User getCurrentStoreStaff(java.security.Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new BadRequestException("Invalid principal");
        }

        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.getName()));

        if (currentUser.getRole() == null || !"FRANCHISE_STORE_STAFF".equalsIgnoreCase(currentUser.getRole().getName())) {
            throw new BadRequestException("Only franchise store staff can perform this action");
        }

        Store store = currentUser.getStore();
        if (store == null) {
            throw new BadRequestException("Store staff is not assigned to any store");
        }

        return currentUser;
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int nonNullInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private static class StoreRevenueAggregate {
        private final String storeId;
        private final String storeName;
        private BigDecimal totalRevenue;

        private StoreRevenueAggregate(String storeId, String storeName, BigDecimal totalRevenue) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.totalRevenue = totalRevenue;
        }
    }

    private record ParsedSaleRow(String productId, Integer quantity, String unit, BigDecimal unitPrice) {}
}
