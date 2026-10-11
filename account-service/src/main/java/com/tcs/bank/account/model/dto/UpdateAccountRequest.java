package com.tcs.bank.account.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.account.model.enumeration.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @JsonProperty("numeroCuenta")
                @NotBlank
                @Pattern(regexp = "[0-9]{1,50}", message = "debe contener solo dígitos")
                @Size(max = 50)
                String accountNumber,
        @JsonProperty("tipoCuenta") @NotNull AccountType accountType,
        @JsonProperty("estado") @NotNull Boolean status) {
}
