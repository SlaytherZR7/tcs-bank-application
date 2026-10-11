package com.tcs.bank.account.service;

import com.tcs.bank.account.model.dto.AccountStatementReportItem;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    List<AccountStatementReportItem> generateAccountStatement(
            Long clientId, LocalDate startDate, LocalDate endDate);
}
