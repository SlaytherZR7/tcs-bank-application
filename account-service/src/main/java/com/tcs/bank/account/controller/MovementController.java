package com.tcs.bank.account.controller;

import com.tcs.bank.account.model.dto.CreateMovementRequest;
import com.tcs.bank.account.model.dto.MovementResponse;
import com.tcs.bank.account.model.dto.UpdateMovementRequest;
import com.tcs.bank.account.service.MovementService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movimientos")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public ResponseEntity<List<MovementResponse>> findAll(
            @RequestParam(name = "numeroCuenta", required = false) String accountNumber) {
        return ResponseEntity.ok(movementService.findAll(accountNumber));
    }

    @GetMapping("/{movementId}")
    public ResponseEntity<MovementResponse> findById(@PathVariable Long movementId) {
        return ResponseEntity.ok(movementService.findById(movementId));
    }

    @PostMapping
    public ResponseEntity<MovementResponse> create(
            @Valid @RequestBody CreateMovementRequest request) {
        MovementResponse response = movementService.create(request);
        return ResponseEntity.created(
                URI.create("/api/movimientos/" + response.movementId()))
                .body(response);
    }

    @PutMapping("/{movementId}")
    public ResponseEntity<MovementResponse> replace(
            @PathVariable Long movementId,
            @Valid @RequestBody UpdateMovementRequest request) {
        return ResponseEntity.ok(movementService.replace(movementId, request));
    }

    @PatchMapping("/{movementId}")
    public ResponseEntity<MovementResponse> patch(
            @PathVariable Long movementId,
            @Valid @RequestBody UpdateMovementRequest request) {
        return ResponseEntity.ok(movementService.replace(movementId, request));
    }

    @DeleteMapping("/{movementId}")
    public ResponseEntity<Void> compensate(@PathVariable Long movementId) {
        movementService.compensate(movementId);
        return ResponseEntity.noContent().build();
    }
}
