package com.tcs.bank.account.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreateMovementRequest(

        @JsonProperty("numeroCuenta")
                @NotBlank(message = "es obligatorio")
                @Pattern(regexp = "[0-9]{1,50}", message = "debe contener solo dígitos")
                String accountNumber,
        @JsonProperty("valor")
                @NotNull(message = "es obligatorio")
                @Digits(integer = 17, fraction = 2, message = "máximo 17 enteros y 2 decimales")
                BigDecimal valor) {
}
