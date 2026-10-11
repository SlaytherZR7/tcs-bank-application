package com.tcs.bank.account.controller;

import com.tcs.bank.account.exceptions.AccountNotFoundException;
import com.tcs.bank.account.exceptions.ClientNotActiveException;
import com.tcs.bank.account.exceptions.ClientProjectionNotFoundException;
import com.tcs.bank.account.exceptions.DuplicateAccountNumberException;
import com.tcs.bank.account.model.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountNotFound(
            AccountNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ClientProjectionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleClientNotFound(
            ClientProjectionNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ClientNotActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleClientNotActive(
            ClientNotActiveException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateAccountNumberException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateAccountNumber(
            DuplicateAccountNumberException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handlePersistenceConflict(HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "El registro entra en conflicto con una restricción de datos",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidJson(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido", request);
    }

    private String formatFieldError(FieldError error) {
        return jsonFieldName(error.getField()) + ": " + error.getDefaultMessage();
    }

    private String jsonFieldName(String fieldName) {
        return switch (fieldName) {
            case "accountNumber" -> "numeroCuenta";
            case "clientId" -> "clienteId";
            case "accountType" -> "tipoCuenta";
            case "initialBalance" -> "saldoInicial";
            case "status" -> "estado";
            default -> fieldName;
        };
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.from(status, message, request.getRequestURI()));
    }
}
