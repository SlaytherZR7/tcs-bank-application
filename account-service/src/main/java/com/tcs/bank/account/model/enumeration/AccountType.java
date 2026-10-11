package com.tcs.bank.account.model.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum AccountType {
    SAVINGS("Ahorro"),
    CHECKING("Corriente");

    private final String jsonValue;

    AccountType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }

    @JsonCreator
    public static AccountType fromJson(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ahorro", "ahorros" -> SAVINGS;
            case "corriente" -> CHECKING;
            default -> throw new IllegalArgumentException("Tipo de cuenta inválido: " + value);
        };
    }
}
