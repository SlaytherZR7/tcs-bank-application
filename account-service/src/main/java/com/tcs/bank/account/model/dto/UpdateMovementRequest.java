package com.tcs.bank.account.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateMovementRequest(

        /**
         * New signed amount for the corrected movement.
         * Positive = deposit, negative = withdrawal.
         */
        @JsonProperty("valor")
        @NotNull(message = "es obligatorio")
        @Digits(integer = 17, fraction = 2, message = "máximo 17 enteros y 2 decimales")
        BigDecimal valor) {
}
