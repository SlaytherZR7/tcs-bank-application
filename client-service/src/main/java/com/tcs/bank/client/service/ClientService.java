package com.tcs.bank.client.service;

import com.tcs.bank.client.model.dto.ClientRequest;
import com.tcs.bank.client.model.dto.ClientResponse;
import com.tcs.bank.client.model.dto.PatchClientRequest;
import java.util.List;

public interface ClientService {

    List<ClientResponse> findAll();

    ClientResponse findById(Long clientId);

    ClientResponse create(ClientRequest request);

    ClientResponse replace(Long clientId, ClientRequest request);

    ClientResponse patch(Long clientId, PatchClientRequest request);

    void softDelete(Long clientId);
}
