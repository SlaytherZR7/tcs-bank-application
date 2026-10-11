package com.tcs.bank.account.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.account.model.enumeration.AccountType;
import java.math.BigDecimal;

public record AccountStatementReportItem(
        @JsonProperty("Fecha") String date,
        @JsonProperty("Cliente") String clientName,
        @JsonProperty("Numero Cuenta") String accountNumber,
        @JsonProperty("Tipo") AccountType accountType,
        @JsonProperty("Saldo Inicial") BigDecimal initialBalance,
        @JsonProperty("Estado") Boolean status,
        @JsonProperty("Movimiento") BigDecimal amount,
        @JsonProperty("Saldo Disponible") BigDecimal resultingBalance) {
}
