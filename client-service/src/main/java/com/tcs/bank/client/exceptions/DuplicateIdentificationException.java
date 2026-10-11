package com.tcs.bank.client.exceptions;

public class DuplicateIdentificationException extends RuntimeException {

    public DuplicateIdentificationException() {
        super("Ya existe un cliente con esa identificación");
    }
}
