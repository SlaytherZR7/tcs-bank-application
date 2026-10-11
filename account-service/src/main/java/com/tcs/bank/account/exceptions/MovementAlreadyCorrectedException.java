package com.tcs.bank.account.exceptions;

public class MovementAlreadyCorrectedException extends RuntimeException {

    public MovementAlreadyCorrectedException(Long movementId) {
        super("El movimiento con ID " + movementId + " ya fue corregido o anulado previamente");
    }
}
