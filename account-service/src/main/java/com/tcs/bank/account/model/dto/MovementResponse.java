package com.tcs.bank.account.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.account.model.enumeration.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovementResponse(
        @JsonProperty("movimientoId") Long movementId,
        @JsonProperty("fecha") LocalDateTime movementDate,
        @JsonProperty("tipoMovimiento") MovementType movementType,
        @JsonProperty("valor") BigDecimal amount,
        @JsonProperty("saldo") BigDecimal resultingBalance,
        @JsonProperty("numeroCuenta") String accountNumber) {
}
