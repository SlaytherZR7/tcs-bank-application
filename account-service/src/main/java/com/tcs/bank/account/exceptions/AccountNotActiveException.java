package com.tcs.bank.account.exceptions;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(String accountNumber) {
        super("La cuenta " + accountNumber + " no está activa");
    }
}
