package com.tcs.bank.account.repository;

import com.tcs.bank.account.model.entity.ClientProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientProjectionRepository extends JpaRepository<ClientProjection, Long> {
}
