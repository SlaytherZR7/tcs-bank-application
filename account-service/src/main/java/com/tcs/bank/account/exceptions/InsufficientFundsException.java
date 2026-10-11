package com.tcs.bank.account.exceptions;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("Saldo no disponible");
    }
}
