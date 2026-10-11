package com.tcs.bank.client.controller;

import com.tcs.bank.client.model.dto.ClientRequest;
import com.tcs.bank.client.model.dto.ClientResponse;
import com.tcs.bank.client.model.dto.PatchClientRequest;
import com.tcs.bank.client.service.ClientService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> findAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ClientResponse> findById(@PathVariable Long clientId) {
        return ResponseEntity.ok(clientService.findById(clientId));
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        ClientResponse client = clientService.create(request);
        return ResponseEntity.created(URI.create("/api/clientes/" + client.clientId())).body(client);
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ClientResponse> replace(
            @PathVariable Long clientId,
            @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.ok(clientService.replace(clientId, request));
    }

    @PatchMapping("/{clientId}")
    public ResponseEntity<ClientResponse> patch(
            @PathVariable Long clientId,
            @Valid @RequestBody PatchClientRequest request) {
        return ResponseEntity.ok(clientService.patch(clientId, request));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(@PathVariable Long clientId) {
        clientService.softDelete(clientId);
        return ResponseEntity.noContent().build();
    }
}
