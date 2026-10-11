package com.tcs.bank.account.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.account.model.enumeration.AccountType;
import java.math.BigDecimal;

public record AccountResponse(
        @JsonProperty("cuentaId") Long accountId,
        @JsonProperty("numeroCuenta") String accountNumber,
        @JsonProperty("clienteId") Long clientId,
        @JsonProperty("cliente") String clientName,
        @JsonProperty("tipoCuenta") AccountType accountType,
        @JsonProperty("saldoInicial") BigDecimal initialBalance,
        @JsonProperty("saldoDisponible") BigDecimal availableBalance,
        @JsonProperty("estado") boolean status) {
}
