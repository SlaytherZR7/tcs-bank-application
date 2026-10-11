package com.tcs.bank.account.exceptions;

public class ClientNotActiveException extends RuntimeException {

    public ClientNotActiveException(Long clientId) {
        super("El cliente con ID " + clientId + " no está activo");
    }
}
