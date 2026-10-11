package com.tcs.bank.account.exceptions;

public class MovementNotFoundException extends RuntimeException {

    public MovementNotFoundException(Long id) {
        super("No se encontró el movimiento con ID " + id);
    }
}
