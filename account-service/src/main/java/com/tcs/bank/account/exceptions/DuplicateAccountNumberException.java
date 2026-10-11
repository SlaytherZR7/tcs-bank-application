package com.tcs.bank.account.exceptions;

public class DuplicateAccountNumberException extends RuntimeException {

    public DuplicateAccountNumberException() {
        super("Ya existe una cuenta con ese número");
    }
}
