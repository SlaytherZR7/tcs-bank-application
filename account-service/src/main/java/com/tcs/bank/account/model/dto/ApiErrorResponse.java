package com.tcs.bank.account.model.dto;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        Instant timestamp, int status, String error, String message, String path) {

    public static ApiErrorResponse from(HttpStatus status, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
