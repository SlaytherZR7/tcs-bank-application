package com.tcs.bank.account.exceptions;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long accountId) {
        super("No se encontró la cuenta con ID " + accountId);
    }
}
