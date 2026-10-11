package com.tcs.bank.account.exceptions;

public class ClientProjectionNotFoundException extends RuntimeException {

    public ClientProjectionNotFoundException(Long clientId) {
        super("No se encontró el cliente con ID " + clientId);
    }
}
