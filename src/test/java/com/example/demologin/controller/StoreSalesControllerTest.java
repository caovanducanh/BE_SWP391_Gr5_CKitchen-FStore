package com.example.demologin.controller;

import com.example.demologin.dto.response.SalesReportClearResponse;
import com.example.demologin.dto.response.SalesReportImportResponse;
import com.example.demologin.dto.response.StoreDailySalesDetailResponse;
import com.example.demologin.dto.response.StoreDailySalesResponse;
import com.example.demologin.service.SalesReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreSalesControllerTest {

    @Mock
    private SalesReportService salesReportService;

    @InjectMocks
    private StoreSalesController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void downloadTemplate_shouldReturnExcelAttachment() {
        Principal principal = () -> "store_staff_01";
        byte[] expected = new byte[] {1, 2, 3};
        when(salesReportService.exportTemplate(principal)).thenReturn(expected);

        ResponseEntity<byte[]> result = controller.downloadTemplate(principal);

        assertEquals(200, result.getStatusCode().value());
        assertEquals("attachment; filename=sales_report_template.xlsx",
                result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                result.getHeaders().getContentType().toString());
        assertArrayEquals(expected, result.getBody());
        verify(salesReportService).exportTemplate(principal);
    }

    @Test
    void importSalesReport_shouldInvokeService() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sales.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {9, 9}
        );
        LocalDate reportDate = LocalDate.of(2026, 4, 20);
        Principal principal = () -> "store_staff_01";

        SalesReportImportResponse response = SalesReportImportResponse.builder()
                .reportId("SR00000001")
                .build();
        when(salesReportService.importSalesReport(file, reportDate, principal)).thenReturn(response);

        Object result = controller.importSalesReport(file, reportDate, principal);

        assertSame(response, result);
        verify(salesReportService).importSalesReport(file, reportDate, principal);
    }

    @Test
    void clearSalesReport_shouldInvokeService() {
        LocalDate reportDate = LocalDate.of(2026, 4, 20);
        Principal principal = () -> "store_staff_01";
        SalesReportClearResponse response = SalesReportClearResponse.builder()
                .reportId("SR00000001")
                .build();

        when(salesReportService.clearSalesReport(reportDate, principal)).thenReturn(response);

        Object result = controller.clearSalesReport(reportDate, principal);

        assertSame(response, result);
        verify(salesReportService).clearSalesReport(reportDate, principal);
    }

    @Test
    void getMyDailySales_shouldInvokeService() {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 4, 30);
        Principal principal = () -> "store_staff_01";
        Page<StoreDailySalesResponse> page = new PageImpl<>(List.of());

        when(salesReportService.getMyDailySales(fromDate, toDate, principal, 0, 20)).thenReturn(page);

        Object result = controller.getMyDailySales(fromDate, toDate, 0, 20, principal);

        assertSame(page, result);
        verify(salesReportService).getMyDailySales(fromDate, toDate, principal, 0, 20);
    }

    @Test
    void getMyDailySalesDetail_shouldInvokeService() {
        LocalDate reportDate = LocalDate.of(2026, 4, 20);
        Principal principal = () -> "store_staff_01";
        StoreDailySalesDetailResponse response = StoreDailySalesDetailResponse.builder()
                .reportId("SR00000001")
                .build();

        when(salesReportService.getMyDailySalesDetail(reportDate, principal, 0, 20)).thenReturn(response);

        Object result = controller.getMyDailySalesDetail(reportDate, 0, 20, principal);

        assertSame(response, result);
        verify(salesReportService).getMyDailySalesDetail(reportDate, principal, 0, 20);
    }
}
