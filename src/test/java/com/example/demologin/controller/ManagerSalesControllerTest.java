package com.example.demologin.controller;

import com.example.demologin.dto.response.DailyRevenueRangeResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.dto.response.StoreTotalRevenueResponse;
import com.example.demologin.service.SalesReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerSalesControllerTest {

    @Mock
    private SalesReportService salesReportService;

    @InjectMocks
    private ManagerSalesController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDailyRevenue_shouldInvokeService() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        DailyRevenueRangeResponse response = DailyRevenueRangeResponse.builder().dayCount(2).build();

        when(salesReportService.getDailyRevenue(fromDate, toDate, "ST001")).thenReturn(response);

        Object result = controller.getDailyRevenue(fromDate, toDate, "ST001");

        assertSame(response, result);
        verify(salesReportService).getDailyRevenue(fromDate, toDate, "ST001");
    }

    @Test
    void getStoreTotalRevenue_shouldInvokeService() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        StoreTotalRevenueResponse response = StoreTotalRevenueResponse.builder().storeCount(1).build();

        when(salesReportService.getStoreTotalRevenue(fromDate, toDate, null)).thenReturn(response);

        Object result = controller.getStoreTotalRevenue(fromDate, toDate, null);

        assertSame(response, result);
        verify(salesReportService).getStoreTotalRevenue(fromDate, toDate, null);
    }

    @Test
    void getAllStoresForRevenueFilter_shouldInvokeService() {
        List<StoreResponse> response = List.of(StoreResponse.builder().id("ST001").name("Store A").build());
        when(salesReportService.getAllStoresForRevenueFilter()).thenReturn(response);

        Object result = controller.getAllStoresForRevenueFilter();

        assertSame(response, result);
        verify(salesReportService).getAllStoresForRevenueFilter();
    }

    @Test
    void getAllKitchensForRevenueFilter_shouldInvokeService() {
        List<KitchenResponse> response = List.of(KitchenResponse.builder().id("KIT001").name("Kitchen A").build());
        when(salesReportService.getAllKitchensForRevenueFilter()).thenReturn(response);

        Object result = controller.getAllKitchensForRevenueFilter();

        assertSame(response, result);
        verify(salesReportService).getAllKitchensForRevenueFilter();
    }

    @Test
    void exportDailyRevenue_shouldReturnExcelAttachment() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        byte[] expected = new byte[] {10, 20, 30};
        when(salesReportService.exportDailyRevenueReport(fromDate, toDate, "ST001")).thenReturn(expected);

        ResponseEntity<byte[]> result = controller.exportDailyRevenue(fromDate, toDate, "ST001");

        assertEquals(200, result.getStatusCode().value());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                result.getHeaders().getContentType().toString());
        String contentDisposition = result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertEquals(true, contentDisposition != null && contentDisposition.startsWith("attachment; filename=manager_daily_revenue_"));
        assertArrayEquals(expected, result.getBody());
        verify(salesReportService).exportDailyRevenueReport(fromDate, toDate, "ST001");
    }
}
