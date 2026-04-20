package com.example.demologin.service;

import com.example.demologin.dto.response.DailyRevenueReportResponse;
import com.example.demologin.dto.response.DailyRevenueRangeResponse;
import com.example.demologin.dto.response.SalesReportClearResponse;
import com.example.demologin.dto.response.SalesReportImportResponse;
import com.example.demologin.dto.response.KitchenResponse;
import com.example.demologin.dto.response.StoreDailySalesDetailResponse;
import com.example.demologin.dto.response.StoreDailySalesResponse;
import com.example.demologin.dto.response.StoreRevenueItemResponse;
import com.example.demologin.dto.response.StoreResponse;
import com.example.demologin.dto.response.StoreTotalRevenueResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

public interface SalesReportService {
    byte[] exportTemplate(Principal principal);

    SalesReportImportResponse importSalesReport(MultipartFile file, LocalDate reportDate, Principal principal);

    SalesReportClearResponse clearSalesReport(LocalDate reportDate, Principal principal);

    Page<StoreDailySalesResponse> getMyDailySales(LocalDate fromDate, LocalDate toDate, Principal principal, int page, int size);

    StoreDailySalesDetailResponse getMyDailySalesDetail(LocalDate reportDate, Principal principal, int page, int size);

    DailyRevenueRangeResponse getDailyRevenue(LocalDate fromDate, LocalDate toDate, String storeId);

    StoreTotalRevenueResponse getStoreTotalRevenue(LocalDate fromDate, LocalDate toDate, String storeId);

    List<StoreResponse> getAllStoresForRevenueFilter();

    List<KitchenResponse> getAllKitchensForRevenueFilter();

    byte[] exportDailyRevenueReport(LocalDate fromDate, LocalDate toDate, String storeId);
}
