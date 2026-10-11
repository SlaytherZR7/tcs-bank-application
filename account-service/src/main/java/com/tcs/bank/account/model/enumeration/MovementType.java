package com.tcs.bank.account.model.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.util.Locale;

public enum MovementType {
    INCOME("Ingreso"),
    EXPENSE("Egreso");

    private final String jsonValue;

    MovementType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String jsonValue() {
        return jsonValue;
    }

    public static MovementType fromAmount(BigDecimal amount) {
        if (amount == null || BigDecimal.ZERO.compareTo(amount) == 0) {
            throw new IllegalArgumentException("El monto del movimiento no puede ser nulo o cero");
        }
        return amount.signum() > 0 ? INCOME : EXPENSE;
    }

    @JsonCreator
    public static MovementType fromJson(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ingreso" -> INCOME;
            case "egreso" -> EXPENSE;
            default -> throw new IllegalArgumentException("Tipo de movimiento inválido: " + value);
        };
    }
}
