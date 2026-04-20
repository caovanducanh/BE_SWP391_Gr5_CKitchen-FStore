package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.DailyRevenueRangeResponse;
import com.example.demologin.dto.response.SalesReportClearResponse;
import com.example.demologin.dto.response.SalesReportImportResponse;
import com.example.demologin.dto.response.StoreDailySalesDetailResponse;
import com.example.demologin.dto.response.StoreDailySalesResponse;
import com.example.demologin.dto.response.StoreTotalRevenueResponse;
import com.example.demologin.entity.Kitchen;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Role;
import com.example.demologin.entity.SaleItem;
import com.example.demologin.entity.SalesRecord;
import com.example.demologin.entity.Store;
import com.example.demologin.entity.StoreInventory;
import com.example.demologin.entity.User;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.BusinessException;
import com.example.demologin.exception.exceptions.ConflictException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.KitchenRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.SaleItemRepository;
import com.example.demologin.repository.SalesRecordRepository;
import com.example.demologin.repository.StoreInventoryRepository;
import com.example.demologin.repository.StoreRepository;
import com.example.demologin.repository.UserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceImplTest {

    @Mock
    private SalesRecordRepository salesRecordRepository;
    @Mock
    private SaleItemRepository saleItemRepository;
    @Mock
    private StoreInventoryRepository storeInventoryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private KitchenRepository kitchenRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SalesReportServiceImpl salesReportService;

    @Test
    void exportTemplate_shouldThrowWhenPrincipalIsInvalid() {
        assertThrows(BadRequestException.class, () -> salesReportService.exportTemplate(null));
    }

    @Test
    void exportTemplate_shouldThrowWhenPrincipalNameIsNull() {
        Principal principal = Mockito.mock(Principal.class);
        when(principal.getName()).thenReturn(null);

        assertThrows(BadRequestException.class, () -> salesReportService.exportTemplate(principal));
    }

    @Test
    void exportTemplate_shouldThrowWhenPrincipalNameIsBlank() {
        Principal principal = () -> "   ";

        assertThrows(BadRequestException.class, () -> salesReportService.exportTemplate(principal));
    }

    @Test
    void exportTemplate_shouldThrowWhenUserNotFound() {
        Principal principal = principal("staff01");
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> salesReportService.exportTemplate(principal));
    }

    @Test
    void exportTemplate_shouldThrowWhenRoleIsNotStoreStaff() {
        Principal principal = principal("manager01");
        User manager = buildUser("manager01", "MANAGER", null);
        when(userRepository.findByUsername("manager01")).thenReturn(Optional.of(manager));

        assertThrows(BadRequestException.class, () -> salesReportService.exportTemplate(principal));
    }

    @Test
    void exportTemplate_shouldThrowWhenRoleIsNull() {
        Principal principal = principal("staff01");
        User user = new User();
        user.setUsername("staff01");
        user.setStore(Store.builder().id("ST001").name("Store 1").address("Address").status("ACTIVE").build());
        user.setRole(null);
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> salesReportService.exportTemplate(principal));
    }

    @Test
    void exportTemplate_shouldThrowWhenStoreNotAssigned() {
        Principal principal = principal("staff01");
        User staffWithoutStore = buildUser("staff01", "FRANCHISE_STORE_STAFF", null);
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(staffWithoutStore));

        assertThrows(BadRequestException.class, () -> salesReportService.exportTemplate(principal));
    }

    @Test
    void exportTemplate_shouldReturnExcelBytes() {
        Principal principal = principal("staff01");
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));

        byte[] bytes = salesReportService.exportTemplate(principal);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void exportTemplate_shouldWrapIOExceptionWhenWriteFails() throws Exception {
        Principal principal = principal("staff01");
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));

        SalesReportServiceImpl spyService = Mockito.spy(salesReportService);
        Mockito.doThrow(new IOException("write failed"))
                .when(spyService)
                .writeWorkbookToBytes(any(Workbook.class), any(ByteArrayOutputStream.class));

        assertThrows(BusinessException.class, () -> spyService.exportTemplate(principal));
    }

    @Test
    void importSalesReport_shouldThrowWhenFileMissing() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(null, LocalDate.now(), principal("staff01")));
    }

    @Test
    void importSalesReport_shouldThrowWhenFileEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "sales.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(emptyFile, LocalDate.now(), principal("staff01")));
    }

    @Test
    void importSalesReport_shouldThrowWhenReportDateMissing() {
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD001", 1, "pcs", 25000}});

        assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, null, principal("staff01")));
    }

    @Test
    void importSalesReport_shouldThrowWhenReportAlreadyExists() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD001", 1, "pcs", 25000}});
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
    }

    @Test
    void importSalesReport_shouldThrowWhenNoDataRows() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{});
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
    }

    @Test
    void importSalesReport_shouldThrowWhenQuantityIsNotInteger() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD001", 1.5, "pcs", 25000}});

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
        assertTrue(ex.getMessage().contains("quantity must be a positive integer"));
    }

    @Test
    void importSalesReport_shouldThrowWhenProductUnknown() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD999", 1, "pcs", 25000}});
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
    }

    @Test
    void importSalesReport_shouldThrowWhenInventoryMissing() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD001", 1, "pcs", 25000}});
        Product product = Product.builder().id("PROD001").name("Bread").unit("pcs").build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(product));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
    }

    @Test
    void importSalesReport_shouldThrowWhenSoldQuantityExceedsStock() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD001", 5, "pcs", 25000}});
        Product product = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        StoreInventory inventory = StoreInventory.builder().store(Store.builder().id("ST001").build()).product(product).quantity(3).unit("pcs").minStock(0).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(product));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of(inventory));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
        assertTrue(ex.getMessage().contains("sold 5 but stock is only 3"));
    }

    @Test
    void importSalesReport_shouldThrowWhenUnitMismatch() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD001", 2, "bottle", 25000}});
        Product product = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        StoreInventory inventory = StoreInventory.builder().store(Store.builder().id("ST001").build()).product(product).quantity(5).unit("pcs").minStock(0).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(product));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of(inventory));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
        assertTrue(ex.getMessage().contains("inventory unit is"));
    }

    @Test
    void importSalesReport_shouldThrowWhenExcelReadFailsWithIOException() throws Exception {
        Principal principal = principal("staff01");
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
    }

    @Test
    void importSalesReport_shouldThrowWhenExcelFormatInvalid() {
        Principal principal = principal("staff01");
        MockMultipartFile file = new MockMultipartFile("file", "sales.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "not-an-excel".getBytes());

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
    }

        @Test
        void importSalesReport_shouldThrowInvalidFormatWhenWorkbookHasNoSheet() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithoutSheet();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);

        assertThrows(BadRequestException.class,
            () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));
        }

        @Test
        void importSalesReport_shouldCollectMultipleRowErrors() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{
            {"", 1, "pcs", 25000},
            {"PROD001", 1, "", 25000},
            {"PROD001", 1, "pcs", -1}
        });

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));

        assertTrue(ex.getMessage().contains("product_id is required"));
        assertTrue(ex.getMessage().contains("unit is required"));
        assertTrue(ex.getMessage().contains("unit_price must be >= 0"));
        }

    @Test
    void importSalesReport_shouldRejectQuantityZeroAndMissingPrice() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{
                {"PROD001", 0, "pcs", 25000},
                {"PROD001", 1, "pcs", ""}
        });

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal));

        assertTrue(ex.getMessage().contains("quantity must be a positive integer"));
        assertTrue(ex.getMessage().contains("unit_price must be >= 0"));
    }

    @Test
    void importSalesReport_shouldSkipNullRowsInSheet() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithSparseRows();
        Product prod1 = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        StoreInventory inv1 = StoreInventory.builder().store(Store.builder().id("ST001").build()).product(prod1).quantity(10).unit("pcs").minStock(0).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(prod1));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of(inv1));
        when(salesRecordRepository.findAllIds()).thenReturn(List.of("SR00000001"));

        SalesReportImportResponse result = salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal);

        assertEquals(1, result.getItemCount());
        assertEquals(1, result.getTotalQuantity());
    }

    @Test
    void importSalesReport_shouldPassWhenUnitsAllMatch() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{{"PROD001", 2, "pcs", 25000}});
        Product product = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        StoreInventory inventory = StoreInventory.builder().store(Store.builder().id("ST001").build()).product(product).quantity(10).unit("pcs").minStock(0).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(product));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of(inventory));
        when(salesRecordRepository.findAllIds()).thenReturn(List.of("SR00000009"));

        SalesReportImportResponse result = salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal);

        assertEquals("SR00000010", result.getReportId());
        assertEquals(8, inventory.getQuantity());
    }

    @Test
    void importSalesReport_shouldSkipTrulyNullIntermediateRows() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithNullIntermediateRows();
        Product prod1 = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        StoreInventory inv1 = StoreInventory.builder().store(Store.builder().id("ST001").build()).product(prod1).quantity(10).unit("pcs").minStock(0).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(prod1));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of(inv1));
        when(salesRecordRepository.findAllIds()).thenReturn(List.of("SR00000001"));

        SalesReportImportResponse result = salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal);

        assertEquals(1, result.getItemCount());
        assertEquals(1, result.getTotalQuantity());
    }

    @Test
    void importSalesReport_shouldImportAndUpdateInventorySuccessfully() {
        Principal principal = principal("staff01");
        MockMultipartFile file = excelFileWithRows(new Object[][]{
                {"PROD001", 2, "pcs", 25000},
                {"PROD001", 1, "pcs", 25000},
                {"PROD002", 3, "box", 15000}
        });

        Product prod1 = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        Product prod2 = Product.builder().id("PROD002").name("Cake").unit("box").build();

        StoreInventory inv1 = StoreInventory.builder().store(Store.builder().id("ST001").build()).product(prod1).quantity(10).unit("pcs").minStock(0).build();
        StoreInventory inv2 = StoreInventory.builder().store(Store.builder().id("ST001").build()).product(prod2).quantity(20).unit("box").minStock(0).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.existsByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(false);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(prod1, prod2));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of(inv1, inv2));
        when(salesRecordRepository.findAllIds()).thenReturn(Arrays.asList("SR00000002", "BAD_ID", null));

        SalesReportImportResponse result = salesReportService.importSalesReport(file, LocalDate.of(2026, 4, 20), principal);

        assertEquals("SR00000003", result.getReportId());
        assertEquals("ST001", result.getStoreId());
        assertEquals(3, result.getItemCount());
        assertEquals(6, result.getTotalQuantity());
        assertEquals(0, result.getTotalRevenue().compareTo(new BigDecimal("120000")));

        assertEquals(7, inv1.getQuantity());
        assertEquals(17, inv2.getQuantity());

        verify(salesRecordRepository).save(any(SalesRecord.class));
        verify(saleItemRepository).saveAll(anyList());
        verify(storeInventoryRepository).saveAll(anyCollection());
    }

    @Test
    void clearSalesReport_shouldThrowWhenDateMissing() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.clearSalesReport(null, principal("staff01")));
    }

    @Test
    void clearSalesReport_shouldThrowWhenNoRecordFound() {
        Principal principal = principal("staff01");
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> salesReportService.clearSalesReport(LocalDate.of(2026, 4, 20), principal));
    }

    @Test
    void clearSalesReport_shouldWorkWhenNoSaleItems() {
        Principal principal = principal("staff01");
        Store store = Store.builder().id("ST001").name("Store 1").build();
        SalesRecord record = SalesRecord.builder().id("SR00000009").store(store).date(LocalDate.of(2026, 4, 20)).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(Optional.of(record));
        when(saleItemRepository.findBySaleId("SR00000009")).thenReturn(List.of());

        SalesReportClearResponse result = salesReportService.clearSalesReport(LocalDate.of(2026, 4, 20), principal);

        assertEquals(0, result.getRestoredItems());
        assertEquals(0, result.getRestoredQuantity());
        verify(saleItemRepository).deleteBySaleId("SR00000009");
        verify(salesRecordRepository).delete(record);
    }

    @Test
    void clearSalesReport_shouldRestoreAndDeleteSuccessfully() {
        Principal principal = principal("staff01");
        Store store = Store.builder().id("ST001").name("Store 1").build();
        SalesRecord record = SalesRecord.builder().id("SR00000009").store(store).date(LocalDate.of(2026, 4, 20)).build();
        Product prod1 = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        Product prod2 = Product.builder().id("PROD002").name("Cake").unit("box").build();
        SaleItem item1 = SaleItem.builder().sale(record).product(prod1).quantity(2).unit("pcs").unitPrice(new BigDecimal("10000")).build();
        SaleItem item2 = SaleItem.builder().sale(record).product(prod2).quantity(3).unit("box").unitPrice(new BigDecimal("20000")).build();

        StoreInventory inv1 = StoreInventory.builder().store(store).product(prod1).quantity(5).unit("pcs").minStock(0).build();
        StoreInventory inv2 = StoreInventory.builder().store(store).product(prod2).quantity(7).unit("box").minStock(0).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(Optional.of(record));
        when(saleItemRepository.findBySaleId("SR00000009")).thenReturn(List.of(item1, item2));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of(inv1, inv2));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(prod1, prod2));

        SalesReportClearResponse result = salesReportService.clearSalesReport(LocalDate.of(2026, 4, 20), principal);

        assertEquals("SR00000009", result.getReportId());
        assertEquals(2, result.getRestoredItems());
        assertEquals(5, result.getRestoredQuantity());
        assertEquals(7, inv1.getQuantity());
        assertEquals(10, inv2.getQuantity());

        verify(storeInventoryRepository).saveAll(anyCollection());
        verify(saleItemRepository).deleteBySaleId("SR00000009");
        verify(salesRecordRepository).delete(record);
    }

    @Test
    void clearSalesReport_shouldCreateMissingInventoryOnRestore() {
        Principal principal = principal("staff01");
        Store store = Store.builder().id("ST001").name("Store 1").build();
        SalesRecord record = SalesRecord.builder().id("SR00000010").store(store).date(LocalDate.of(2026, 4, 20)).build();
        Product prod1 = Product.builder().id("PROD001").name("Bread").unit("pcs").build();
        SaleItem item1 = SaleItem.builder().sale(record).product(prod1).quantity(4).unit("pcs").unitPrice(new BigDecimal("10000")).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(Optional.of(record));
        when(saleItemRepository.findBySaleId("SR00000010")).thenReturn(List.of(item1));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of());
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(prod1));

        SalesReportClearResponse result = salesReportService.clearSalesReport(LocalDate.of(2026, 4, 20), principal);

        assertEquals(1, result.getRestoredItems());
        assertEquals(4, result.getRestoredQuantity());
        verify(storeInventoryRepository).saveAll(anyCollection());
    }

    @Test
    void clearSalesReport_shouldSkipUnknownProductDuringRestore() {
        Principal principal = principal("staff01");
        Store store = Store.builder().id("ST001").name("Store 1").build();
        SalesRecord record = SalesRecord.builder().id("SR00000011").store(store).date(LocalDate.of(2026, 4, 20)).build();
        Product missingProduct = Product.builder().id("PROD404").name("Missing").unit("pcs").build();
        SaleItem item = SaleItem.builder().sale(record).product(missingProduct).quantity(2).unit("pcs").unitPrice(new BigDecimal("10000")).build();

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(Optional.of(record));
        when(saleItemRepository.findBySaleId("SR00000011")).thenReturn(List.of(item));
        when(storeInventoryRepository.findByStoreIdAndProductIdIn(eq("ST001"), anyCollection())).thenReturn(List.of());
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of());

        SalesReportClearResponse result = salesReportService.clearSalesReport(LocalDate.of(2026, 4, 20), principal);

        assertEquals(2, result.getRestoredQuantity());
        verify(storeInventoryRepository).saveAll(anyCollection());
    }

    @Test
    void getMyDailySales_shouldThrowWhenInvalidRange() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.getMyDailySales(LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 1), principal("staff01"), 0, 20));
    }

    @Test
    void getMyDailySales_shouldThrowWhenPageInvalid() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.getMyDailySales(null, null, principal("staff01"), -1, 20));
    }

    @Test
    void getMyDailySales_shouldThrowWhenSizeInvalid() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.getMyDailySales(null, null, principal("staff01"), 0, 0));
    }

    @Test
    void getMyDailySales_shouldMapPageContent() {
        Principal principal = principal("staff01");
        Store store = Store.builder().id("ST001").name("Store 1").build();
        SalesRecord record = SalesRecord.builder()
                .id("SR00000012")
                .store(store)
                .date(LocalDate.of(2026, 4, 20))
                .totalRevenue(null)
                .recordedBy("staff01")
                .recordedAt(LocalDateTime.now())
                .build();
        Page<SalesRecord> page = new PageImpl<>(List.of(record));

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDateRange(eq("ST001"), eq(null), eq(null), any())).thenReturn(page);
        when(saleItemRepository.countBySaleId("SR00000012")).thenReturn(2L);
        when(saleItemRepository.sumQuantityBySaleId("SR00000012")).thenReturn(null);

        Page<StoreDailySalesResponse> result = salesReportService.getMyDailySales(null, null, principal, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(BigDecimal.ZERO, result.getContent().get(0).getTotalRevenue());
        assertEquals(2, result.getContent().get(0).getItemCount());
        assertEquals(0, result.getContent().get(0).getTotalQuantity());
    }

    @Test
    void getMyDailySales_shouldAcceptEqualDateRange() {
        Principal principal = principal("staff01");
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDateRange(eq("ST001"), eq(LocalDate.of(2026, 4, 20)), eq(LocalDate.of(2026, 4, 20)), any()))
                .thenReturn(Page.empty());

        Page<StoreDailySalesResponse> result = salesReportService.getMyDailySales(
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 20),
                principal,
                0,
                20
        );

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getMyDailySales_shouldAllowOpenEndedRanges() {
        Principal principal = principal("staff01");
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDateRange(eq("ST001"), eq(LocalDate.of(2026, 4, 1)), eq(null), any()))
                .thenReturn(Page.empty());
        when(salesRecordRepository.findByStoreIdAndDateRange(eq("ST001"), eq(null), eq(LocalDate.of(2026, 4, 30)), any()))
                .thenReturn(Page.empty());

        Page<StoreDailySalesResponse> result1 = salesReportService.getMyDailySales(LocalDate.of(2026, 4, 1), null, principal, 0, 20);
        Page<StoreDailySalesResponse> result2 = salesReportService.getMyDailySales(null, LocalDate.of(2026, 4, 30), principal, 0, 20);

        assertEquals(0, result1.getTotalElements());
        assertEquals(0, result2.getTotalElements());
    }

    @Test
    void getMyDailySalesDetail_shouldThrowWhenDateMissing() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.getMyDailySalesDetail(null, principal("staff01"), 0, 20));
    }

    @Test
    void getMyDailySalesDetail_shouldThrowWhenPageInvalid() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.getMyDailySalesDetail(LocalDate.of(2026, 4, 20), principal("staff01"), -1, 20));
    }

    @Test
    void getMyDailySalesDetail_shouldThrowWhenSizeInvalid() {
        assertThrows(BadRequestException.class,
                () -> salesReportService.getMyDailySalesDetail(LocalDate.of(2026, 4, 20), principal("staff01"), 0, 0));
    }

    @Test
    void getMyDailySalesDetail_shouldThrowWhenRecordNotFound() {
        Principal principal = principal("staff01");
        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> salesReportService.getMyDailySalesDetail(LocalDate.of(2026, 4, 20), principal, 0, 20));
    }

    @Test
    void getMyDailySalesDetail_shouldMapPagedItems() {
        Principal principal = principal("staff01");
        Store store = Store.builder().id("ST001").name("Store 1").build();
        SalesRecord record = SalesRecord.builder()
                .id("SR00000013")
                .store(store)
                .date(LocalDate.of(2026, 4, 20))
                .totalRevenue(new BigDecimal("70000"))
                .recordedBy("staff01")
                .recordedAt(LocalDateTime.now())
                .build();
        Product product = Product.builder().id("PROD001").name("Bread").build();
        SaleItem item = SaleItem.builder().id(1).sale(record).product(product).quantity(2).unit("pcs").unitPrice(new BigDecimal("35000")).build();
        Page<SaleItem> itemPage = new PageImpl<>(List.of(item));

        when(userRepository.findByUsername("staff01")).thenReturn(Optional.of(validStoreStaff()));
        when(salesRecordRepository.findByStoreIdAndDate("ST001", LocalDate.of(2026, 4, 20))).thenReturn(Optional.of(record));
        when(saleItemRepository.findBySaleId(eq("SR00000013"), any())).thenReturn(itemPage);
        when(saleItemRepository.sumQuantityBySaleId("SR00000013")).thenReturn(2);

        StoreDailySalesDetailResponse result = salesReportService.getMyDailySalesDetail(LocalDate.of(2026, 4, 20), principal, 0, 20);

        assertEquals("SR00000013", result.getReportId());
        assertEquals("ST001", result.getStoreId());
        assertEquals(1, result.getItems().size());
        assertEquals(0, result.getTotalRevenue().compareTo(new BigDecimal("70000")));
        assertEquals(0, result.getItems().get(0).getLineTotal().compareTo(new BigDecimal("70000")));
        assertFalse(result.getHasNext());
    }

    @Test
    void getDailyRevenue_shouldThrowWhenFromDateAfterToDate() {
        LocalDate fromDate = LocalDate.of(2026, 5, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 1);

        assertThrows(BadRequestException.class,
                () -> salesReportService.getDailyRevenue(fromDate, toDate, null));
    }

    @Test
    void getDailyRevenue_shouldGroupByDateAndAggregateTotals() {
        LocalDate day1 = LocalDate.of(2026, 4, 20);
        LocalDate day2 = LocalDate.of(2026, 4, 19);
        when(salesRecordRepository.summarizeDailyRevenue(null, null, null)).thenReturn(List.of(
                dailyProjection(day1, "ST001", "Store 1", new BigDecimal("25000"), 1L),
                dailyProjection(day1, "ST002", "Store 2", new BigDecimal("50000"), 2L),
                dailyProjection(day2, "ST001", "Store 1", new BigDecimal("10000"), 1L)
        ));

        DailyRevenueRangeResponse result = salesReportService.getDailyRevenue(null, null, null);

        assertEquals(2, result.getDayCount());
        assertEquals(0, result.getTotalRevenue().compareTo(new BigDecimal("85000")));

        assertEquals(day1, result.getDays().get(0).getReportDate());
        assertEquals(2, result.getDays().get(0).getStoreCount());
        assertEquals(0, result.getDays().get(0).getTotalRevenue().compareTo(new BigDecimal("75000")));

        assertEquals(day2, result.getDays().get(1).getReportDate());
        assertEquals(1, result.getDays().get(1).getStoreCount());
        assertEquals(0, result.getDays().get(1).getTotalRevenue().compareTo(new BigDecimal("10000")));
    }

    @Test
    void getDailyRevenue_shouldTreatBlankStoreIdAsNull() {
        when(salesRecordRepository.summarizeDailyRevenue(null, null, null)).thenReturn(List.of());

        DailyRevenueRangeResponse result = salesReportService.getDailyRevenue(null, null, "   ");

        assertEquals(0, result.getDayCount());
        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
        verify(salesRecordRepository).summarizeDailyRevenue(null, null, null);
    }

    @Test
    void getDailyRevenue_shouldAcceptEqualDateRangeAndStoreFilter() {
        LocalDate date = LocalDate.of(2026, 4, 20);
        when(salesRecordRepository.summarizeDailyRevenue(date, date, "ST001")).thenReturn(List.of());

        DailyRevenueRangeResponse result = salesReportService.getDailyRevenue(date, date, "ST001");

        assertEquals(0, result.getDayCount());
        verify(salesRecordRepository).summarizeDailyRevenue(date, date, "ST001");
    }

    @Test
    void getDailyRevenue_shouldAllowOpenEndedRanges() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        when(salesRecordRepository.summarizeDailyRevenue(fromDate, null, null)).thenReturn(List.of());
        when(salesRecordRepository.summarizeDailyRevenue(null, toDate, null)).thenReturn(List.of());

        DailyRevenueRangeResponse r1 = salesReportService.getDailyRevenue(fromDate, null, null);
        DailyRevenueRangeResponse r2 = salesReportService.getDailyRevenue(null, toDate, null);

        assertEquals(0, r1.getDayCount());
        assertEquals(0, r2.getDayCount());
    }

    @Test
    void getStoreTotalRevenue_shouldThrowWhenFromDateAfterToDate() {
        LocalDate fromDate = LocalDate.of(2026, 5, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 1);

        assertThrows(BadRequestException.class,
                () -> salesReportService.getStoreTotalRevenue(fromDate, toDate, null));
    }

    @Test
    void getStoreTotalRevenue_shouldReturnStoreBreakdownWhenStoreIdMissing() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        when(salesRecordRepository.summarizeRevenueByStore(fromDate, toDate)).thenReturn(List.of(
                storeProjection("ST001", "Store 1", new BigDecimal("25000")),
                storeProjection("ST002", "Store 2", new BigDecimal("50000"))
        ));

        StoreTotalRevenueResponse result = salesReportService.getStoreTotalRevenue(fromDate, toDate, "   ");

        assertEquals(2, result.getStoreCount());
        assertEquals(0, result.getTotalReportRevenue().compareTo(new BigDecimal("75000")));
        assertEquals(2, result.getStores().size());
        assertEquals("ST001", result.getStores().get(0).getStoreId());
        assertEquals("Store 1", result.getStores().get(0).getStoreName());

        verify(salesRecordRepository).summarizeRevenueByStore(fromDate, toDate);
        verify(salesRecordRepository, never()).sumRevenueByStoreAndDateRange(any(), any(), any());
    }

    @Test
    void getStoreTotalRevenue_shouldReturnSingleStoreListWhenStoreIdProvided() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);

        when(salesRecordRepository.sumRevenueByStoreAndDateRange("ST001", fromDate, toDate))
                .thenReturn(new BigDecimal("25000"));
        when(storeRepository.findById("ST001"))
                .thenReturn(Optional.of(Store.builder().id("ST001").name("Store District 1").build()));

        StoreTotalRevenueResponse result = salesReportService.getStoreTotalRevenue(fromDate, toDate, " ST001 ");

        assertEquals(1, result.getStoreCount());
        assertEquals(0, result.getTotalReportRevenue().compareTo(new BigDecimal("25000")));
        assertEquals(1, result.getStores().size());
        assertEquals("ST001", result.getStores().get(0).getStoreId());
        assertEquals("Store District 1", result.getStores().get(0).getStoreName());
        assertEquals(0, result.getStores().get(0).getTotalReportRevenue().compareTo(new BigDecimal("25000")));
    }

    @Test
    void getStoreTotalRevenue_shouldHandleNullRevenueAndUnknownStoreName() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);

        when(salesRecordRepository.sumRevenueByStoreAndDateRange("ST003", fromDate, toDate)).thenReturn(null);
        when(storeRepository.findById("ST003")).thenReturn(Optional.empty());

        StoreTotalRevenueResponse result = salesReportService.getStoreTotalRevenue(fromDate, toDate, "ST003");

        assertEquals(1, result.getStoreCount());
        assertEquals(BigDecimal.ZERO, result.getTotalReportRevenue());
        assertNull(result.getStores().get(0).getStoreName());
    }

    @Test
    void getStoreTotalRevenue_shouldAcceptEqualDateRange() {
        LocalDate date = LocalDate.of(2026, 4, 20);
        when(salesRecordRepository.sumRevenueByStoreAndDateRange("ST001", date, date)).thenReturn(BigDecimal.ZERO);
        when(storeRepository.findById("ST001")).thenReturn(Optional.of(Store.builder().id("ST001").name("Store 1").build()));

        StoreTotalRevenueResponse result = salesReportService.getStoreTotalRevenue(date, date, "ST001");

        assertEquals(1, result.getStoreCount());
    }

    @Test
    void getStoreTotalRevenue_shouldAllowOpenEndedRanges() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        when(salesRecordRepository.summarizeRevenueByStore(fromDate, null)).thenReturn(List.of());
        when(salesRecordRepository.summarizeRevenueByStore(null, toDate)).thenReturn(List.of());

        StoreTotalRevenueResponse r1 = salesReportService.getStoreTotalRevenue(fromDate, null, null);
        StoreTotalRevenueResponse r2 = salesReportService.getStoreTotalRevenue(null, toDate, "   ");

        assertEquals(0, r1.getStoreCount());
        assertEquals(0, r2.getStoreCount());
    }

    @Test
    void getAllStoresForRevenueFilter_shouldMapStoreFields() {
        Store store = Store.builder()
                .id("ST001")
                .name("Store A")
                .address("Address")
                .phone("0909")
                .manager("Manager A")
                .status("ACTIVE")
                .openDate(LocalDate.of(2024, 1, 1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(storeRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(store));

        var result = salesReportService.getAllStoresForRevenueFilter();

        assertEquals(1, result.size());
        assertEquals("ST001", result.get(0).getId());
        assertEquals("Store A", result.get(0).getName());
    }

    @Test
    void getAllKitchensForRevenueFilter_shouldMapKitchenFields() {
        Kitchen kitchen = Kitchen.builder()
                .id("KIT001")
                .name("Kitchen A")
                .address("Address")
                .phone("0909")
                .capacity(100)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(kitchenRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(kitchen));

        var result = salesReportService.getAllKitchensForRevenueFilter();

        assertEquals(1, result.size());
        assertEquals("KIT001", result.get(0).getId());
        assertEquals("Kitchen A", result.get(0).getName());
    }

    @Test
    void exportDailyRevenueReport_shouldReturnExcelBytes() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);

        when(salesRecordRepository.summarizeDailyRevenue(fromDate, toDate, null)).thenReturn(List.of(
                dailyProjection(LocalDate.of(2026, 4, 20), null, null, new BigDecimal("25000"), 1L),
                dailyProjection(LocalDate.of(2026, 4, 19), "ST001", "", new BigDecimal("15000"), 1L)
        ));

        byte[] bytes = salesReportService.exportDailyRevenueReport(fromDate, toDate, null);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void exportDailyRevenueReport_shouldWrapIOExceptionWhenWriteFails() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        when(salesRecordRepository.summarizeDailyRevenue(fromDate, toDate, null)).thenReturn(List.of());

        SalesReportServiceImpl spyService = Mockito.spy(salesReportService);
        Mockito.doThrow(new IOException("write failed"))
                .when(spyService)
                .writeWorkbookToBytes(any(Workbook.class), any(ByteArrayOutputStream.class));

        assertThrows(BusinessException.class, () -> spyService.exportDailyRevenueReport(fromDate, toDate, null));
    }

    @Test
    void exportDailyRevenueReport_shouldTrimStoreFilter() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);

        when(salesRecordRepository.summarizeDailyRevenue(fromDate, toDate, "ST001")).thenReturn(List.of(
                dailyProjection(LocalDate.of(2026, 4, 20), "ST001", "Store A", new BigDecimal("25000"), 1L)
        ));

        byte[] bytes = salesReportService.exportDailyRevenueReport(fromDate, toDate, " ST001 ");

        assertNotNull(bytes);
        verify(salesRecordRepository).summarizeDailyRevenue(fromDate, toDate, "ST001");
    }

    @Test
    void exportDailyRevenueReport_shouldCoverLabelBranchesWithNullAndBlankFields() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        when(salesRecordRepository.summarizeDailyRevenue(fromDate, toDate, null)).thenReturn(List.of(
                dailyProjection(null, null, null, null, 1L),
                dailyProjection(LocalDate.of(2026, 4, 20), "STX", "", new BigDecimal("10000"), 1L),
                dailyProjection(LocalDate.of(2026, 4, 20), "STY", "Store Y", new BigDecimal("20000"), 1L)
        ));

        byte[] bytes = salesReportService.exportDailyRevenueReport(fromDate, toDate, null);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void exportDailyRevenueReport_shouldHandleBlankStoreIdKeyBranch() {
        when(salesRecordRepository.summarizeDailyRevenue(null, null, null)).thenReturn(List.of(
                dailyProjection(LocalDate.of(2026, 4, 20), " ", "Blank Id Store", new BigDecimal("10000"), 1L)
        ));

        byte[] bytes = salesReportService.exportDailyRevenueReport(null, null, null);

        assertNotNull(bytes);
    }

    @Test
    void exportDailyRevenueReport_shouldHandleNullStoreNameWhenStoreIdExists() {
        when(salesRecordRepository.summarizeDailyRevenue(null, null, null)).thenReturn(List.of(
                dailyProjection(LocalDate.of(2026, 4, 20), "STN", null, new BigDecimal("30000"), 1L)
        ));

        byte[] bytes = salesReportService.exportDailyRevenueReport(null, null, null);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void exportDailyRevenueReport_shouldCoverUnknownStoreNameBranch() {
        when(salesRecordRepository.summarizeDailyRevenue(null, null, null)).thenReturn(List.of(
                dailyProjection(LocalDate.of(2026, 4, 20), null, "Unknown Name", new BigDecimal("10000"), 1L)
        ));

        byte[] bytes = salesReportService.exportDailyRevenueReport(null, null, null);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void invokeReadDateCellViaReflection_shouldHandleNumericStringAndInvalid() throws Exception {
        Method method = SalesReportServiceImpl.class.getDeclaredMethod("readDateCell", Cell.class, DataFormatter.class);
        method.setAccessible(true);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("s");
            Row row = sheet.createRow(0);

            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(java.sql.Date.valueOf("2026-04-20"));
            CellStyle style = workbook.createCellStyle();
            CreationHelper helper = workbook.getCreationHelper();
            style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
            dateCell.setCellStyle(style);

            Cell textCell = row.createCell(1);
            textCell.setCellValue("2026-04-21");

            Cell invalidCell = row.createCell(2);
            invalidCell.setCellValue("not-a-date");

            Cell blankCell = row.createCell(3);
            blankCell.setCellValue(" ");

            Cell numericNonDate = row.createCell(4);
            numericNonDate.setCellValue(12345);

            LocalDate fromNull = (LocalDate) method.invoke(salesReportService, null, new DataFormatter());
            LocalDate fromNumeric = (LocalDate) method.invoke(salesReportService, dateCell, new DataFormatter());
            LocalDate fromString = (LocalDate) method.invoke(salesReportService, textCell, new DataFormatter());
            LocalDate fromInvalid = (LocalDate) method.invoke(salesReportService, invalidCell, new DataFormatter());
            LocalDate fromBlank = (LocalDate) method.invoke(salesReportService, blankCell, new DataFormatter());
            LocalDate fromNumericNonDate = (LocalDate) method.invoke(salesReportService, numericNonDate, new DataFormatter());

            assertNull(fromNull);
            assertEquals(LocalDate.of(2026, 4, 20), fromNumeric);
            assertEquals(LocalDate.of(2026, 4, 21), fromString);
            assertNull(fromInvalid);
            assertNull(fromBlank);
            assertNull(fromNumericNonDate);
        }
    }

    @Test
    void invokeCellHelpersViaReflection_shouldCoverNullAndInvalidBranches() throws Exception {
        Method readStringCell = SalesReportServiceImpl.class.getDeclaredMethod("readStringCell", Cell.class, DataFormatter.class);
        Method readIntegerCell = SalesReportServiceImpl.class.getDeclaredMethod("readIntegerCell", Cell.class, DataFormatter.class);
        Method readDecimalCell = SalesReportServiceImpl.class.getDeclaredMethod("readDecimalCell", Cell.class, DataFormatter.class);
        Method isRowEmpty = SalesReportServiceImpl.class.getDeclaredMethod("isRowEmpty", Row.class);
        Method generateSalesRecordId = SalesReportServiceImpl.class.getDeclaredMethod("generateSalesRecordId");

        readStringCell.setAccessible(true);
        readIntegerCell.setAccessible(true);
        readDecimalCell.setAccessible(true);
        isRowEmpty.setAccessible(true);
        generateSalesRecordId.setAccessible(true);

        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("s");
            Row row = sheet.createRow(0);
            Cell intCell = row.createCell(0);
            intCell.setCellValue(2);

            Cell decimalQuantityCell = row.createCell(1);
            decimalQuantityCell.setCellValue(2.5);

            Cell textIntCell = row.createCell(2);
            textIntCell.setCellValue("10");

            Cell invalidIntCell = row.createCell(3);
            invalidIntCell.setCellValue("abc");

            Cell blankTextCell = row.createCell(4);
            blankTextCell.setCellValue(" ");

            Cell decimalTextCell = row.createCell(5);
            decimalTextCell.setCellValue("12.5");

            Cell blankTypeCell = row.createCell(6);
            blankTypeCell.setBlank();

            Row emptyRow = sheet.createRow(1);
            emptyRow.createCell(0).setBlank();
            Row nonEmptyRow = sheet.createRow(2);
            nonEmptyRow.createCell(0).setCellValue("PROD001");

            assertEquals("", readStringCell.invoke(salesReportService, null, formatter));
            assertEquals(null, readIntegerCell.invoke(salesReportService, null, formatter));
            assertEquals(2, readIntegerCell.invoke(salesReportService, intCell, formatter));
            assertEquals(null, readIntegerCell.invoke(salesReportService, decimalQuantityCell, formatter));
            assertEquals(10, readIntegerCell.invoke(salesReportService, textIntCell, formatter));
            assertEquals(null, readIntegerCell.invoke(salesReportService, invalidIntCell, formatter));
            assertEquals(null, readIntegerCell.invoke(salesReportService, blankTextCell, formatter));

            assertEquals(null, readDecimalCell.invoke(salesReportService, null, formatter));
            assertEquals(new BigDecimal("2.0"), readDecimalCell.invoke(salesReportService, intCell, formatter));
            assertEquals(null, readDecimalCell.invoke(salesReportService, blankTextCell, formatter));
            assertEquals(new BigDecimal("12.5"), readDecimalCell.invoke(salesReportService, decimalTextCell, formatter));
            assertEquals(null, readDecimalCell.invoke(salesReportService, invalidIntCell, formatter));

            assertEquals(true, isRowEmpty.invoke(salesReportService, (Object) null));
            assertEquals(true, isRowEmpty.invoke(salesReportService, emptyRow));
            assertEquals(false, isRowEmpty.invoke(salesReportService, nonEmptyRow));
            assertEquals("", readStringCell.invoke(salesReportService, blankTypeCell, formatter));
        }

        when(salesRecordRepository.findAllIds()).thenReturn(List.of());
        String nextId = (String) generateSalesRecordId.invoke(salesReportService);
        assertEquals("SR00000001", nextId);
    }

    @Test
    void invokeExtractSequenceViaReflection_shouldHandleInvalidIds() throws Exception {
        Method method = SalesReportServiceImpl.class.getDeclaredMethod("extractSequence", String.class);
        method.setAccessible(true);

        int v1 = (int) method.invoke(null, "SR00000123");
        int v2 = (int) method.invoke(null, "BAD");
        int v3 = (int) method.invoke(null, (Object) null);
        int v4 = (int) method.invoke(null, "SR999999999999999999");

        assertEquals(123, v1);
        assertEquals(0, v2);
        assertEquals(0, v3);
        assertEquals(0, v4);
    }

    private SalesRecordRepository.DailyRevenueProjection dailyProjection(
            LocalDate reportDate,
            String storeId,
            String storeName,
            BigDecimal totalRevenue,
            Long reportCount
    ) {
        return new SalesRecordRepository.DailyRevenueProjection() {
            @Override
            public LocalDate getReportDate() {
                return reportDate;
            }

            @Override
            public String getStoreId() {
                return storeId;
            }

            @Override
            public String getStoreName() {
                return storeName;
            }

            @Override
            public BigDecimal getTotalRevenue() {
                return totalRevenue;
            }

            @Override
            public Long getReportCount() {
                return reportCount;
            }
        };
    }

    private SalesRecordRepository.StoreRevenueProjection storeProjection(
            String storeId,
            String storeName,
            BigDecimal totalRevenue
    ) {
        return new SalesRecordRepository.StoreRevenueProjection() {
            @Override
            public String getStoreId() {
                return storeId;
            }

            @Override
            public String getStoreName() {
                return storeName;
            }

            @Override
            public BigDecimal getTotalRevenue() {
                return totalRevenue;
            }
        };
    }

    private User validStoreStaff() {
        Store store = Store.builder().id("ST001").name("Store 1").status("ACTIVE").address("Address").build();
        return buildUser("staff01", "FRANCHISE_STORE_STAFF", store);
    }

    private User buildUser(String username, String roleName, Store store) {
        User user = new User();
        user.setUsername(username);
        user.setStore(store);
        Role role = Role.builder().name(roleName).permissions(new HashSet<>()).build();
        user.setRole(role);
        return user;
    }

    private Principal principal(String name) {
        return () -> name;
    }

    private MockMultipartFile excelFileWithRows(Object[][] rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("sales_report");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("product_id");
            header.createCell(1).setCellValue("quantity");
            header.createCell(2).setCellValue("unit");
            header.createCell(3).setCellValue("unit_price");

            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                Object[] values = rows[i];
                row.createCell(0).setCellValue(String.valueOf(values[0]));
                if (values[1] instanceof Number number) {
                    row.createCell(1).setCellValue(number.doubleValue());
                } else {
                    row.createCell(1).setCellValue(String.valueOf(values[1]));
                }
                row.createCell(2).setCellValue(String.valueOf(values[2]));
                if (values[3] instanceof Number number) {
                    row.createCell(3).setCellValue(number.doubleValue());
                } else {
                    row.createCell(3).setCellValue(String.valueOf(values[3]));
                }
            }

            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "sales.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private MockMultipartFile excelFileWithoutSheet() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "sales.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private MockMultipartFile excelFileWithSparseRows() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("sales_report");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("product_id");
            header.createCell(1).setCellValue("quantity");
            header.createCell(2).setCellValue("unit");
            header.createCell(3).setCellValue("unit_price");

            sheet.createRow(1);

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("PROD001");
            row2.createCell(1).setCellValue(1);
            row2.createCell(2).setCellValue("pcs");
            row2.createCell(3).setCellValue(25000);

            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "sales_sparse.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private MockMultipartFile excelFileWithNullIntermediateRows() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("sales_report");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("product_id");
            header.createCell(1).setCellValue("quantity");
            header.createCell(2).setCellValue("unit");
            header.createCell(3).setCellValue("unit_price");

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("PROD001");
            row3.createCell(1).setCellValue(1);
            row3.createCell(2).setCellValue("pcs");
            row3.createCell(3).setCellValue(25000);

            workbook.write(out);
            return new MockMultipartFile(
                    "file",
                    "sales_null_rows.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
