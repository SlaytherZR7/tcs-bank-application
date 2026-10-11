package com.tcs.bank.client.repository;

import com.tcs.bank.client.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByIdentification(String identification);

    boolean existsByIdentificationAndIdNot(String identification, Long clientId);
}
