package com.tcs.bank.client.service.impl;

import com.tcs.bank.client.exceptions.ClientNotFoundException;
import com.tcs.bank.client.exceptions.DuplicateIdentificationException;
import com.tcs.bank.client.mappers.ClientMapper;
import com.tcs.bank.client.model.dto.ClientRequest;
import com.tcs.bank.client.model.dto.ClientResponse;
import com.tcs.bank.client.model.dto.PatchClientRequest;
import com.tcs.bank.client.model.entity.Client;
import com.tcs.bank.client.model.enumeration.ClientStatus;
import com.tcs.bank.client.repository.ClientRepository;
import com.tcs.bank.client.service.ClientService;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final PasswordEncoder passwordEncoder;

    public ClientServiceImpl(
            ClientRepository clientRepository,
            ClientMapper clientMapper,
            PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<ClientResponse> findAll() {
        return clientRepository.findAll()
                .stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    @Override
    public ClientResponse findById(Long clientId) {
        return clientMapper.toResponse(getClient(clientId));
    }

    @Override
    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = clientMapper.toEntity(request);
        ensureIdentificationAvailable(client.getIdentification(), null);
        client.setPasswordHash(passwordEncoder.encode(request.password()));
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientResponse replace(Long clientId, ClientRequest request) {
        Client client = getClient(clientId);
        clientMapper.replace(client, request);
        ensureIdentificationAvailable(client.getIdentification(), clientId);
        client.setPasswordHash(passwordEncoder.encode(request.password()));
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientResponse patch(Long clientId, PatchClientRequest request) {
        Client client = getClient(clientId);
        clientMapper.patch(request, client);
        if (request.identification() != null) {
            ensureIdentificationAvailable(client.getIdentification(), clientId);
        }
        if (request.password() != null) {
            client.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Override
    @Transactional
    public void softDelete(Long clientId) {
        Client client = getClient(clientId);
        if (client.getStatus() != ClientStatus.DELETED) {
            client.markDeleted();
            clientRepository.save(client);
        }
    }

    private Client getClient(Long clientId) {
        return clientRepository
                .findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    private void ensureIdentificationAvailable(String identification, Long currentClientId) {
        boolean identificationAlreadyUsed = currentClientId == null
                ? clientRepository.existsByIdentification(identification)
                : clientRepository.existsByIdentificationAndIdNot(identification, currentClientId);
        if (identificationAlreadyUsed) {
            throw new DuplicateIdentificationException();
        }
    }
}
