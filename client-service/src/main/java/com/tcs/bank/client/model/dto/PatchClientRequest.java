package com.tcs.bank.client.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.client.model.enumeration.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PatchClientRequest(
        @JsonProperty("nombre") @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") @Size(max = 100) String name,
        @JsonProperty("genero") Gender gender,
        @JsonProperty("edad") @Positive @Max(130) Integer age,
        @JsonProperty("identificacion") @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") @Size(max = 20) String identification,
        @JsonProperty("direccion") @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") @Size(max = 255) String address,
        @JsonProperty("telefono") @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") @Size(max = 20) String phone,
        @JsonProperty("contrasena") @Pattern(regexp = ".*\\S.*", message = "no puede estar vacío") @Size(min = 4, max = 72) String password,
        @JsonProperty("estado") Boolean status) {
}
