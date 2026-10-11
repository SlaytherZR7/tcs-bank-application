package com.tcs.bank.account.controller;

import com.tcs.bank.account.model.dto.AccountResponse;
import com.tcs.bank.account.model.dto.CreateAccountRequest;
import com.tcs.bank.account.model.dto.PatchAccountRequest;
import com.tcs.bank.account.model.dto.UpdateAccountRequest;
import com.tcs.bank.account.service.AccountService;
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
@RequestMapping("/api/cuentas")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> findAll(
            @RequestParam(name = "clienteId", required = false) Long clientId) {
        return ResponseEntity.ok(accountService.findAll(clientId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> findById(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.findById(accountId));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse account = accountService.create(request);
        return ResponseEntity.created(URI.create("/api/cuentas/" + account.accountId())).body(account);
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<AccountResponse> replace(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.replace(accountId, request));
    }

    @PatchMapping("/{accountId}")
    public ResponseEntity<AccountResponse> patch(
            @PathVariable Long accountId,
            @Valid @RequestBody PatchAccountRequest request) {
        return ResponseEntity.ok(accountService.patch(accountId, request));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> delete(@PathVariable Long accountId) {
        accountService.softDelete(accountId);
        return ResponseEntity.noContent().build();
    }
}
