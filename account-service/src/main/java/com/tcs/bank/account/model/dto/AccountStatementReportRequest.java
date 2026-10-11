package com.tcs.bank.account.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.BindParam;

public record AccountStatementReportRequest(
        @BindParam("clienteId") @NotNull(message = "es obligatorio") @Positive(message = "debe ser positivo") Long clientId,
        @BindParam("fechaInicio") @NotNull(message = "es obligatoria") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @BindParam("fechaFin") @NotNull(message = "es obligatoria") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

    @AssertTrue(message = "no puede ser posterior a fechaFin")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
