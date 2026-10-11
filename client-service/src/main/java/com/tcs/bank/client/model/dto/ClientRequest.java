package com.tcs.bank.client.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.client.model.enumeration.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @JsonProperty("nombre") @NotBlank @Size(max = 100) String name,
        @JsonProperty("genero") @NotNull Gender gender,
        @JsonProperty("edad") @NotNull @Positive @Max(130) Integer age,
        @JsonProperty("identificacion") @NotBlank @Size(max = 20) String identification,
        @JsonProperty("direccion") @NotBlank @Size(max = 255) String address,
        @JsonProperty("telefono") @NotBlank @Size(max = 20) String phone,
        @JsonProperty("contrasena") @NotBlank @Size(min = 4, max = 72) String password,
        @JsonProperty("estado") @NotNull Boolean status) {
}
