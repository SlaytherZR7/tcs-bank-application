package com.tcs.bank.client.exceptions;

public class ClientNotFoundException extends RuntimeException {

	public ClientNotFoundException(Long clientId) {
		super("No se encontró el cliente con ID " + clientId);
	}
}
