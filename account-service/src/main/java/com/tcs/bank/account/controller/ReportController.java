package com.tcs.bank.account.controller;

import com.tcs.bank.account.model.dto.AccountStatementReportItem;
import com.tcs.bank.account.model.dto.AccountStatementReportRequest;
import com.tcs.bank.account.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<List<AccountStatementReportItem>> getReport(
            @Valid @ModelAttribute AccountStatementReportRequest request) {

        List<AccountStatementReportItem> report = reportService.generateAccountStatement(
                request.clientId(), request.startDate(), request.endDate());
        return ResponseEntity.ok(report);
    }
}
