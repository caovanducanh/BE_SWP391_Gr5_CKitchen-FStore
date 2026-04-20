package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.service.SalesReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store/sales")
@Tag(name = "Store Sales Report", description = "APIs for store staff to report daily sales")
public class StoreSalesController {

    private final SalesReportService salesReportService;

    @GetMapping("/template")
    @SecuredEndpoint("SALES_REPORT_TEMPLATE_DOWNLOAD")
    @Operation(summary = "Download sales report template", description = "Download Excel template for daily sales report upload")
    public ResponseEntity<byte[]> downloadTemplate(Principal principal) {
        byte[] bytes = salesReportService.exportTemplate(principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_report_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(message = "Sales report imported successfully")
    @SecuredEndpoint("SALES_REPORT_IMPORT")
    @Operation(summary = "Import daily sales report", description = "Import daily sales Excel file after store closing and validate with store inventory")
    public Object importSalesReport(
            @RequestPart("file") MultipartFile file,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            Principal principal
    ) {
        return salesReportService.importSalesReport(file, reportDate, principal);
    }

    @DeleteMapping
    @ApiResponse(message = "Sales report cleared successfully")
    @SecuredEndpoint("SALES_REPORT_CLEAR")
    @Operation(summary = "Clear daily sales report", description = "Clear sales report for a date to allow re-import and restore sold quantity back to inventory")
    public Object clearSalesReport(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            Principal principal
    ) {
        return salesReportService.clearSalesReport(reportDate, principal);
    }

    @GetMapping("/daily")
    @PageResponse
    @ApiResponse(message = "Daily sales reports retrieved successfully")
    @SecuredEndpoint("SALES_REPORT_VIEW_OWN")
    @Operation(summary = "Get my daily sales", description = "Store staff gets daily sales summaries of their own store in selected date range")
    public Object getMyDailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return salesReportService.getMyDailySales(fromDate, toDate, principal, page, size);
    }

    @GetMapping("/daily/detail")
    @ApiResponse(message = "Daily sales detail retrieved successfully")
    @SecuredEndpoint("SALES_REPORT_VIEW_OWN")
    @Operation(summary = "Get my daily sales detail", description = "Store staff views detailed sold items for a specific date of their own store")
    public Object getMyDailySalesDetail(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        return salesReportService.getMyDailySalesDetail(reportDate, principal, page, size);
    }
}
