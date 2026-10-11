package com.tcs.bank.account.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.account.model.enumeration.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateAccountRequest(
        @JsonProperty("numeroCuenta")
                @NotBlank
                @Pattern(regexp = "[0-9]{1,50}", message = "debe contener solo dígitos")
                @Size(max = 50)
                String accountNumber,
        @JsonProperty("clienteId") @NotNull @Positive Long clientId,
        @JsonProperty("tipoCuenta") @NotNull AccountType accountType,
        @JsonProperty("saldoInicial") @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal initialBalance,
        @JsonProperty("estado") @NotNull Boolean status) {
}
