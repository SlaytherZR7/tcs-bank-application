package com.tcs.bank.client.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcs.bank.client.model.enumeration.Gender;

public record ClientResponse(
        @JsonProperty("clienteId") Long clientId,
        @JsonProperty("nombre") String name,
        @JsonProperty("genero") Gender gender,
        @JsonProperty("edad") Integer age,
        @JsonProperty("identificacion") String identification,
        @JsonProperty("direccion") String address,
        @JsonProperty("telefono") String phone,
        @JsonProperty("estado") boolean status) {
}
